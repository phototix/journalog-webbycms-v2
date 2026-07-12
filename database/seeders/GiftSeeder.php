<?php

namespace Database\Seeders;

use App\Model\Gift;
use Illuminate\Database\Seeder;

class GiftSeeder extends Seeder
{
    public function run(): void
    {
        $gifts = [
            ['name' => 'Rose', 'icon' => 'rose-outline', 'credits' => 5, 'category' => 'Romantic', 'sort_order' => 1],
            ['name' => 'Heart', 'icon' => 'heart', 'credits' => 10, 'category' => 'Romantic', 'sort_order' => 2],
            ['name' => 'Kiss', 'icon' => 'heart-circle-outline', 'credits' => 20, 'category' => 'Romantic', 'sort_order' => 3],
            ['name' => 'Love Letter', 'icon' => 'mail-open-outline', 'credits' => 15, 'category' => 'Romantic', 'sort_order' => 4],

            ['name' => 'LOL', 'icon' => 'happy-outline', 'credits' => 3, 'category' => 'Funny', 'sort_order' => 1],
            ['name' => 'Clown', 'icon' => 'color-wand-outline', 'credits' => 5, 'category' => 'Funny', 'sort_order' => 2],
            ['name' => 'Party', 'icon' => 'beer-outline', 'credits' => 8, 'category' => 'Funny', 'sort_order' => 3],
            ['name' => 'Fire', 'icon' => 'flame-outline', 'credits' => 12, 'category' => 'Funny', 'sort_order' => 4],

            ['name' => 'Diamond', 'icon' => 'diamond-outline', 'credits' => 50, 'category' => 'Premium', 'sort_order' => 1],
            ['name' => 'Crown', 'icon' => 'crown-outline', 'credits' => 100, 'category' => 'Premium', 'sort_order' => 2],
            ['name' => 'Rocket', 'icon' => 'rocket-outline', 'credits' => 75, 'category' => 'Premium', 'sort_order' => 3],
            ['name' => 'Trophy', 'icon' => 'trophy-outline', 'credits' => 150, 'category' => 'Premium', 'sort_order' => 4],

            ['name' => 'Star', 'icon' => 'star', 'credits' => 25, 'category' => 'Limited-Edition', 'sort_order' => 1],
            ['name' => 'Rainbow', 'icon' => 'color-palette-outline', 'credits' => 30, 'category' => 'Limited-Edition', 'sort_order' => 2],
            ['name' => 'Unicorn', 'icon' => 'planet-outline', 'credits' => 40, 'category' => 'Limited-Edition', 'sort_order' => 3],
        ];

        foreach ($gifts as $gift) {
            Gift::create($gift);
        }
    }
}
