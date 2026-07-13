<?php

namespace App\Http\Controllers;

use App\Providers\AiServiceProvider;
use Illuminate\Http\Request;

class AiController extends Controller
{
    /**
     * Saves license.
     * @param Request $request
     * @return \Illuminate\Http\JsonResponse
     */
    public function generateSuggestion(Request $request) {
        $text = $request->get('text');
        try{
            $suggestion = AiServiceProvider::generateCompletionRequest($text);
            return response()->json(['success' => true, 'message' => $suggestion]);
        }
        catch (\Exception $exception){
            return response()->json(['success' => false, 'errors' => ['An internal error occurred while generating the suggestion.'], 'message' => 'An internal error occurred while generating the suggestion.'], 500);
        }

    }
}
