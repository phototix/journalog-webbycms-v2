<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('gifts', function (Blueprint $table) {
            $table->id();
            $table->string('name');
            $table->string('icon')->comment('CSS icon class');
            $table->string('gif_effect')->nullable()->comment('Path to GIF animation');
            $table->unsignedInteger('credits')->default(1);
            $table->enum('category', ['Romantic', 'Funny', 'Premium', 'Limited-Edition'])->default('Funny');
            $table->boolean('is_active')->default(true);
            $table->unsignedInteger('sort_order')->default(0);
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('gifts');
    }
};
