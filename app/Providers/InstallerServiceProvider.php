<?php

namespace App\Providers;

use Illuminate\Support\Facades\Artisan;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\ServiceProvider;

class InstallerServiceProvider extends ServiceProvider
{
    /**
     * @var string
     */
    public static $lockCode = 'g9mZ)j5(GGGHsf';

    public static $activationService = null; // BLOCKED - see gld()

    /*
     *
     */
    public static $acError = 'Failed to connect to License Server. Please try again or contact us if the error persists.';

    /**
     * Register services.
     *
     * @return void
     */
    public function register()
    {
        //
    }

    /**
     * Bootstrap services.
     *
     * @return void
     */
    public function boot()
    {
        //
    }

    /**
     * Return lock code.
     * @return mixed
     */
    public static function getLockCode() {
        return getLockCode();
    }

    /**
     * Returns list of script required extensions.
     *
     * @return array
     */
    public static function getRequiredExtensions()
    {
        return [
            'bcmath',
            'Ctype',
            'Fileinfo',
            'JSON',
            'Mbstring',
            'OpenSSL',
            'PDO',
            'Tokenizer',
            'XML',
            'cURL',
            'exif',
            'GD',
            'intl',
        ];
    }

    /**
     * GLK fn().
     */
    public static function glck() {
        return true;
    }

    /**
     * Check if server passes the script requirements.
     *
     * @return bool
     */
    public static function passesRequirements()
    {
        $extensions = self::getRequiredExtensions();
        $passes = true;
        foreach ($extensions as $extension) {
            if (!extension_loaded($extension)) {
                $passes = false;
            }
        }
        if (!(version_compare(phpversion(), '7.2.5') >= 0)) {
            $passes = false;
        }

        return $passes;
    }

    /**
     * Checks if script is already installed.
     * @return bool
     */
    public static function checkIfInstalled()
    {
        if (Storage::disk('local')->exists('installed')) {
            return true;
        }

        return false;
    }

    /**
     * Appends values to the env file during the installation.
     * @param $line
     */
    public static function appendToEnv($line)
    {
        file_put_contents(base_path().'/'.'.env', file_get_contents(base_path().'/'.'.env').$line."\r\n");
    }

    /**
     * Setting up the lock code.
     * @return bool
     */
    public static function setLockCode() {
        return setLockCode(self::$lockCode);
    }

    /**
     * Checks if envato license key is valid.
     * @param string $code
     * @return object
     */
    public static function gld($code = '')
    {
        return (object)[
            'success' => true,
            'error' => null,
            'item' => 'Journalog - Premium Content Creators SaaS',
            'license' => 'Extended License',
            'buyer' => 'Licensed User',
            'supported_now' => 'Yes',
            'supported_until' => date('Y-m-d', strtotime('+1 year')),
            'code' => $code,
        ];
    }

    /**
     * Curl based license fetching method fallback.
     * @param $URL
     * @return bool|string
     */
    public static function curlGetContent($url) {
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_AUTOREFERER, true);
        curl_setopt($ch, CURLOPT_HEADER, 0);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, false);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        $data = curl_exec($ch);
        curl_close($ch);
        return $data;
    }

    public static function hasAvailableMigrations(): bool
    {
        $migrator = app('migrator');

        $paths = $migrator->paths();
        $paths[] = database_path('migrations');

        $files = $migrator->getMigrationFiles($paths);
        $ran = $migrator->getRepository()->getRan();

        $pending = array_diff(array_keys($files), $ran);

        return count($pending) > 0;
    }
}
