<?php

namespace App\Console\Commands;

use App\Model\Post;
use App\Model\PostComment;
use App\Model\Subscription;
use App\Model\User;
use Carbon\Carbon;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;

class MigrateOldJournal extends Command
{
    protected $signature = 'migrate:old-journal
                            {--sql=/tmp/old_backup.sql : Path to the old SQL backup file}
                            {--dry-run : Preview changes without inserting}
                            {--limit= : Limit number of records to process}';

    protected $description = 'Migrate users, posts, comments, and follows from old journal backup';

    protected array $userMap = [];
    protected array $postMap = [];
    protected int $totalUsers = 0;
    protected int $totalPosts = 0;
    protected int $totalComments = 0;
    protected int $totalFollows = 0;

    public function handle(): int
    {
        $sqlPath = $this->option('sql');
        $dryRun = $this->option('dry-run');
        $limit = $this->option('limit');

        if (!file_exists($sqlPath)) {
            $this->error("SQL backup not found at: $sqlPath");
            return 1;
        }

        $this->info("Reading backup from: $sqlPath");
        $sql = file_get_contents($sqlPath);

        if ($dryRun) {
            $this->warn('--- DRY RUN MODE ---');
        }

        $this->migrateUsers($sql, $limit, $dryRun);
        $this->migratePosts($sql, $limit, $dryRun);
        $this->migrateComments($sql, $limit, $dryRun);
        $this->migrateFollows($sql, $limit, $dryRun);

        $this->newLine();
        $this->table(
            ['Entity', 'Processed', 'Skipped/Errors'],
            [
                ['Users', $this->totalUsers, ''],
                ['Posts', $this->totalPosts, ''],
                ['Comments', $this->totalComments, ''],
                ['Follows', $this->totalFollows, ''],
            ]
        );

        return 0;
    }

    protected function fixEncoding(?string $text): ?string
    {
        if ($text === null || $text === '') {
            return $text;
        }

        static $reverseMap = null;
        if ($reverseMap === null) {
            $reverseMap = [];
            for ($b = 0; $b <= 0xFF; $b++) {
                $cp = mb_ord(chr($b), 'ISO-8859-1');
                $reverseMap[$cp] = $b;
            }
            $overrides = [
                0x80 => 0x20AC, 0x82 => 0x201A, 0x83 => 0x0192, 0x84 => 0x201E,
                0x85 => 0x2026, 0x86 => 0x2020, 0x87 => 0x2021, 0x88 => 0x02C6,
                0x89 => 0x2030, 0x8A => 0x0160, 0x8B => 0x2039, 0x8C => 0x0152,
                0x8E => 0x017D, 0x91 => 0x2018, 0x92 => 0x2019, 0x93 => 0x201C,
                0x94 => 0x201D, 0x95 => 0x2022, 0x96 => 0x2013, 0x97 => 0x2014,
                0x98 => 0x02DC, 0x99 => 0x2122, 0x9A => 0x0161, 0x9B => 0x203A,
                0x9C => 0x0153, 0x9E => 0x017E, 0x9F => 0x0178,
            ];
            foreach ($overrides as $byte => $cp) {
                $reverseMap[$cp] = $byte;
            }
        }

        $result = '';
        $len = strlen($text);
        $i = 0;
        while ($i < $len) {
            $byte = ord($text[$i]);
            if ($byte < 0x80) {
                if (isset($reverseMap[$byte])) {
                    $result .= chr($reverseMap[$byte]);
                }
                $i++;
            } elseif ($byte < 0xC0) {
                $i++;
            } elseif ($byte < 0xE0) {
                if ($i + 1 >= $len) break;
                $cp = (($byte & 0x1F) << 6) | (ord($text[$i+1]) & 0x3F);
                if (isset($reverseMap[$cp])) {
                    $result .= chr($reverseMap[$cp]);
                }
                $i += 2;
            } elseif ($byte < 0xF0) {
                if ($i + 2 >= $len) break;
                $cp = (($byte & 0x0F) << 12) | ((ord($text[$i+1]) & 0x3F) << 6) | (ord($text[$i+2]) & 0x3F);
                if (isset($reverseMap[$cp])) {
                    $result .= chr($reverseMap[$cp]);
                }
                $i += 3;
            } else {
                $i++;
            }
        }

        if ($result !== '' && $result !== $text && mb_check_encoding($result, 'UTF-8')) {
            return $result;
        }

        return $text;
    }

    protected function parseInsertValues(string $sql, string $tableName): array
    {
        $pattern = '/INSERT INTO `' . preg_quote($tableName, '/') . '`\s*\([^)]+\)\s*VALUES\s*/i';
        $parts = preg_split($pattern, $sql);
        if (count($parts) < 2) {
            return [];
        }

        $rows = [];
        for ($i = 1; $i < count($parts); $i++) {
            $valuesBlock = $parts[$i];
            $semicolonPos = strpos($valuesBlock, ';');
            if ($semicolonPos !== false) {
                $valuesBlock = substr($valuesBlock, 0, $semicolonPos);
            }
            $rows = array_merge($rows, $this->parseValuesBlock($valuesBlock));
        }

        return $rows;
    }

    protected function parseValuesBlock(string $valuesBlock): array
    {
        $rows = [];
        $current = '';
        $depth = 0;
        $inString = false;
        $escape = false;

        for ($i = 0; $i < strlen($valuesBlock); $i++) {
            $ch = $valuesBlock[$i];

            if ($escape) {
                $current .= $ch;
                $escape = false;
                continue;
            }

            if ($ch === '\\' && $inString) {
                $current .= $ch;
                $escape = true;
                continue;
            }

            if ($ch === "'" && !$inString) {
                $inString = true;
                $current .= $ch;
                continue;
            }

            if ($ch === "'" && $inString) {
                $inString = false;
                $current .= $ch;
                continue;
            }

            if (!$inString) {
                if ($ch === '(') {
                    $depth++;
                    if ($depth === 1) {
                        $current = '(';
                        continue;
                    }
                } elseif ($ch === ')') {
                    $depth--;
                    if ($depth === 0) {
                        $current .= ')';
                        $rows[] = $current;
                        continue;
                    }
                }
            }

            $current .= $ch;
        }

        return $rows;
    }

    protected function parseRowValues(string $row, array $columns): array
    {
        $values = [];
        $current = '';
        $inString = false;
        $escape = false;
        $colIndex = 0;
        $row = substr($row, 1, -1);

        for ($i = 0; $i < strlen($row); $i++) {
            $ch = $row[$i];

            if ($escape) {
                if ($ch === "'" || $ch === '\\') {
                    $current .= $ch;
                } else {
                    $current .= '\\' . $ch;
                }
                $escape = false;
                continue;
            }

            if ($ch === '\\' && $inString) {
                $current .= $ch;
                $escape = true;
                continue;
            }

            if ($ch === "'" && !$inString) {
                $inString = true;
                continue;
            }

            if ($ch === "'" && $inString) {
                $inString = false;
                continue;
            }

            if (!$inString && $ch === ',') {
                $colName = $columns[$colIndex] ?? "col_$colIndex";
                $values[$colName] = $current;
                $current = '';
                $colIndex++;
                continue;
            }

            $current .= $ch;
        }

        if ($colIndex < count($columns)) {
            $colName = $columns[$colIndex] ?? "col_$colIndex";
            $values[$colName] = $current;
        }

        return $values;
    }

    protected function getExistingUsernames(): array
    {
        return User::pluck('username')->map(fn($v) => strtolower($v))->toArray();
    }

    protected function migrateUsers(string $sql, ?int $limit, bool $dryRun): void
    {
        $this->newLine();
        $this->info('--- Migrating Users ---');

        $rows = $this->parseInsertValues($sql, 'db_journal_admin');
        if (empty($rows)) {
            $this->warn('No users found in backup.');
            return;
        }

        $columns = ['admin_id', 'admin_username', 'admin_email', 'admin_token', 'admin_password',
            'admin_title', 'admin_desc', 'admin_proffesion', 'admin_name', 'admin_post_day',
            'admin_post_month', 'admin_post_year', 'admin_post_time', 'admin_post_id',
            'admin_stat', 'admin_photo', 'admin_display', 'admin_act_count',
            'admin_act_count_today', 'admin_rank', 'admin_role', 'admin_update', 'admin_language'];

        if ($limit) {
            $rows = array_slice($rows, 0, (int)$limit);
        }

        $existingUsernames = $this->getExistingUsernames();
        $bar = $this->output->createProgressBar(count($rows));
        $bar->start();

        foreach ($rows as $row) {
            $vals = $this->parseRowValues($row, $columns);
            $oldId = (int)$vals['admin_id'];
            $username = $this->fixEncoding($vals['admin_username'] ?? '');
            $name = $this->fixEncoding($vals['admin_name'] ?? '');
            $bio = $this->fixEncoding($vals['admin_desc'] ?? '');
            $email = $this->fixEncoding($vals['admin_email'] ?? '');
            $language = $this->fixEncoding($vals['admin_language'] ?? '');

            if (empty($username)) {
                $username = "olduser_$oldId";
            }

            $originalUsername = $username;

            if (in_array(strtolower($username), $existingUsernames)) {
                $username = $username . '_old' . $oldId;
                $this->warn("Username conflict: '$originalUsername' -> '$username'");
            }

            if (empty($email) || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
                $email = "old_{$username}@old.journal.local";
            }

            if (in_array($email, User::pluck('email')->toArray(), true)) {
                $email = "old_{$username}_{$oldId}@old.journal.local";
            }

            $existingUsernames[] = strtolower($username);

            $userData = [
                'name' => !empty($name) ? $name : $username,
                'username' => $username,
                'email' => $email,
                'password' => bcrypt(Str::random(40)),
                'bio' => !empty($bio) ? $bio : null,
                'email_verified_at' => null,
                'settings' => !empty($language) ? json_encode(['language' => $language]) : null,
                'paid_profile' => false,
                'public_profile' => true,
                'created_at' => Carbon::now(),
                'updated_at' => Carbon::now(),
            ];

            if (!$dryRun) {
                try {
                    $user = User::create($userData);
                    $this->userMap[$oldId] = $user->id;

                    DB::table('old_user_maps')->insert([
                        'old_admin_id' => $oldId,
                        'new_user_id' => $user->id,
                        'created_at' => Carbon::now(),
                        'updated_at' => Carbon::now(),
                    ]);
                } catch (\Exception $e) {
                    $this->error("Failed to create user '$username': " . $e->getMessage());
                    continue;
                }
            }

            $this->totalUsers++;
            $bar->advance();
        }

        $bar->finish();
        $this->newLine();
    }

    protected function migratePosts(string $sql, ?int $limit, bool $dryRun): void
    {
        $this->newLine();
        $this->info('--- Migrating Posts ---');

        $rows = $this->parseInsertValues($sql, 'db_journal_post');
        if (empty($rows)) {
            $this->warn('No posts found in backup.');
            return;
        }

        $columns = ['post_id', 'post_content', 'post_media', 'post_time', 'post_day',
            'post_month', 'post_year', 'post_date', 'post_stat', 'post_display',
            'admin_id', 'post_private', 'post_member'];

        if ($limit) {
            $rows = array_slice($rows, 0, (int)$limit);
        }

        $bar = $this->output->createProgressBar(count($rows));
        $bar->start();

        foreach ($rows as $row) {
            $vals = $this->parseRowValues($row, $columns);
            $oldId = (int)$vals['post_id'];
            $oldUserId = (int)($vals['admin_id'] ?? 0);
            $text = $this->fixEncoding($vals['post_content'] ?? '');
            $postDate = $vals['post_date'] ?? '';
            $postTime = $vals['post_time'] ?? '';
            $postDisplay = (int)($vals['post_display'] ?? 1);
            $postPrivate = (int)($vals['post_private'] ?? 0);

            $newUserId = $this->userMap[$oldUserId] ?? null;
            if (!$newUserId) {
                $bar->advance();
                continue;
            }

            $createdAt = $this->parseDateTime($postDate, $postTime);

            if ($postDisplay === 0) {
                $status = Post::DISAPPROVED_STATUS;
            } else {
                $status = Post::APPROVED_STATUS;
            }

            $postData = [
                'user_id' => $newUserId,
                'text' => !empty($text) ? $text : null,
                'price' => $postPrivate === 1 ? 5.00 : 0,
                'status' => $status,
                'created_at' => $createdAt,
                'updated_at' => $createdAt,
            ];

            if (!$dryRun) {
                try {
                    $post = Post::create($postData);
                    $this->postMap[$oldId] = $post->id;

                    DB::table('old_post_maps')->insert([
                        'old_post_id' => $oldId,
                        'new_post_id' => $post->id,
                        'created_at' => Carbon::now(),
                        'updated_at' => Carbon::now(),
                    ]);
                } catch (\Exception $e) {
                    $this->error("Failed to create post $oldId: " . $e->getMessage());
                    continue;
                }
            }

            $this->totalPosts++;
            $bar->advance();
        }

        $bar->finish();
        $this->newLine();
    }

    protected function migrateComments(string $sql, ?int $limit, bool $dryRun): void
    {
        $this->newLine();
        $this->info('--- Migrating Comments ---');

        $rows = $this->parseInsertValues($sql, 'db_comment');
        if (empty($rows)) {
            $this->warn('No comments found in backup.');
            return;
        }

        $columns = ['comment_id', 'comment_content', 'comment_name', 'comment_admin',
            'post_id', 'comment_time', 'comment_day', 'comment_month', 'comment_year',
            'comment_date', 'comment_stat', 'comment_read', 'post_belong'];

        if ($limit) {
            $rows = array_slice($rows, 0, (int)$limit);
        }

        $bar = $this->output->createProgressBar(count($rows));
        $bar->start();

        foreach ($rows as $row) {
            $vals = $this->parseRowValues($row, $columns);
            $oldPostId = (int)($vals['post_id'] ?? 0);
            $commentAdmin = (int)($vals['comment_admin'] ?? 0);
            $message = $this->fixEncoding($vals['comment_content'] ?? '');
            $commentTime = $vals['comment_time'] ?? '';
            $commentDate = $vals['comment_date'] ?? '';

            $newPostId = $this->postMap[$oldPostId] ?? null;
            if (!$newPostId) {
                $bar->advance();
                continue;
            }

            $newUserId = null;
            if ($commentAdmin > 0 && isset($this->userMap[$commentAdmin])) {
                $newUserId = $this->userMap[$commentAdmin];
            }

            $createdAt = $this->parseDateTime($commentDate, $commentTime);

            if ($newUserId) {
                $commentData = [
                    'user_id' => $newUserId,
                    'post_id' => $newPostId,
                    'message' => $message,
                    'created_at' => $createdAt,
                    'updated_at' => $createdAt,
                ];

                if (!$dryRun) {
                    try {
                        PostComment::create($commentData);
                    } catch (\Exception $e) {
                        $this->error("Failed to create comment on post $oldPostId: " . $e->getMessage());
                        continue;
                    }
                }

                $this->totalComments++;
            }

            $bar->advance();
        }

        $bar->finish();
        $this->newLine();
    }

    protected function migrateFollows(string $sql, ?int $limit, bool $dryRun): void
    {
        $this->newLine();
        $this->info('--- Migrating Follows ---');

        $rows = $this->parseInsertValues($sql, 'db_follow');
        if (empty($rows)) {
            $this->warn('No follows found in backup.');
            return;
        }

        $columns = ['follow_id', 'admin_id', 'journal_id', 'follow_day', 'follow_month',
            'follow_year', 'follow_time', 'follow_stat'];

        if ($limit) {
            $rows = array_slice($rows, 0, (int)$limit);
        }

        $bar = $this->output->createProgressBar(count($rows));
        $bar->start();

        foreach ($rows as $row) {
            $vals = $this->parseRowValues($row, $columns);
            $oldAdminId = (int)($vals['admin_id'] ?? 0);
            $oldJournalId = (int)($vals['journal_id'] ?? 0);
            $followTime = $vals['follow_time'] ?? '';
            $followDay = $vals['follow_day'] ?? '';
            $followMonth = $vals['follow_month'] ?? '';
            $followYear = $vals['follow_year'] ?? '';

            $newSenderId = $this->userMap[$oldAdminId] ?? null;
            $newRecipientId = $this->userMap[$oldJournalId] ?? null;

            if (!$newSenderId || !$newRecipientId) {
                $bar->advance();
                continue;
            }

            $createdAt = Carbon::createFromDate(
                (int)$followYear ?: 2017,
                (int)$followMonth ?: 1,
                (int)$followDay ?: 1
            );

            $subscriptionData = [
                'sender_user_id' => $newSenderId,
                'recipient_user_id' => $newRecipientId,
                'status' => Subscription::ACTIVE_STATUS,
                'amount' => 0,
                'type' => 'free',
                'provider' => 'system',
                'expires_at' => Carbon::create(2037, 12, 31, 23, 59, 59),
                'created_at' => $createdAt,
                'updated_at' => $createdAt,
            ];

            if (!$dryRun) {
                try {
                    Subscription::create($subscriptionData);
                } catch (\Exception $e) {
                    $this->error("Failed to create follow: " . $e->getMessage());
                    continue;
                }
            }

            $this->totalFollows++;
            $bar->advance();
        }

        $bar->finish();
        $this->newLine();
    }

    protected function parseDateTime(string $date, string $time): Carbon
    {
        if (!empty($date) && $date !== '0000-00-00') {
            if (!empty($time)) {
                return Carbon::parse("$date $time");
            }
            return Carbon::parse($date);
        }
        return Carbon::now();
    }
}
