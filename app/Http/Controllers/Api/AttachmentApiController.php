<?php

namespace App\Http\Controllers\Api;

use App\Providers\AttachmentServiceProvider;
use Illuminate\Http\Request;

class AttachmentApiController extends ApiController
{
    public function upload(Request $request, $type)
    {
        $request->validate([
            'file' => 'required|file|mimes:' . getSetting('media.allowed_file_extensions') . '|max:' . ((int) getSetting('media.max_file_upload_size') * 1024),
        ]);

        $file = $request->file('file');
        if (!$file) {
            return $this->error('No file uploaded', 400);
        }

        try {
            $fileMimeType = $file->getMimeType();
            $generateThumbnail = false;
            $generateBlurredPreview = true;
            $applyWatermark = true;

            $directory = AttachmentServiceProvider::getDirectoryByType($fileMimeType);

            if ($type == 'post') {
                $directory = 'posts/' . $directory;
                $generateThumbnail = true;
            } elseif ($type == 'message') {
                $directory = 'messenger/' . $directory;
                $generateThumbnail = true;
            } elseif ($type == 'payment-request') {
                $directory = 'payment-request/' . $directory;
            } elseif ($type == 'story') {
                $directory = 'stories/' . $directory;
                $generateBlurredPreview = false;
                $applyWatermark = false;
                $generateThumbnail = false;
            }

            $attachment = AttachmentServiceProvider::createAttachment(
                $file, $directory, $generateThumbnail, $generateBlurredPreview, $applyWatermark
            );

            return $this->success([
                'attachmentID' => $attachment->id,
                'path' => $attachment->path,
                'type' => AttachmentServiceProvider::getAttachmentType($attachment->type),
                'thumbnail' => $attachment->thumbnail ?? null,
            ], 'Upload successful');
        } catch (\Exception $e) {
            return $this->error($e->getMessage(), 500);
        }
    }
}
