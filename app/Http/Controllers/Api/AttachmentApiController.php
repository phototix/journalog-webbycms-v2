<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\AttachmentController;
use Illuminate\Http\Request;

class AttachmentApiController extends ApiController
{
    public function upload(Request $request, $type)
    {
        $controller = new AttachmentController();
        $result = $controller->upload($request, $type);

        $decoded = json_decode($result->getContent(), true);

        if (isset($decoded['success']) && $decoded['success']) {
            return $this->success([
                'attachmentID' => $decoded['attachmentID'],
                'path' => $decoded['path'],
                'type' => $decoded['type'],
                'thumbnail' => $decoded['thumbnail'] ?? null,
            ], 'Upload successful');
        }

        return $this->error($decoded['message'] ?? 'Upload failed', 400);
    }
}
