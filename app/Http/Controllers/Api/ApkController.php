<?php

namespace App\Http\Controllers\Api;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;

class ApkController extends ApiController
{
    public function version()
    {
        $path = storage_path('app/apk-version.json');

        if (!file_exists($path)) {
            return $this->error('Version info not available', 404);
        }

        $json = json_decode(file_get_contents($path), true);

        return $this->success($json);
    }

    public function latest(Request $request)
    {
        $path = storage_path('app/apk-version.json');

        if (!file_exists($path)) {
            abort(404);
        }

        $json = json_decode(file_get_contents($path), true);

        if (!$json || !isset($json['download_url'])) {
            abort(404);
        }

        return redirect($json['download_url']);
    }
}
