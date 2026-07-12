<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up()
    {
        Schema::create('old_post_maps', function (Blueprint $table) {
            $table->id();
            $table->integer('old_post_id');
            $table->unsignedBigInteger('new_post_id');
            $table->timestamps();
        });
    }

    public function down()
    {
        Schema::dropIfExists('old_post_maps');
    }
};
