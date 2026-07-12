<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up()
    {
        Schema::create('old_user_maps', function (Blueprint $table) {
            $table->id();
            $table->integer('old_admin_id');
            $table->unsignedBigInteger('new_user_id');
            $table->timestamps();
        });
    }

    public function down()
    {
        Schema::dropIfExists('old_user_maps');
    }
};
