@extends('layouts.app')

@section('content')
<div class="container">
    <div class="row justify-content-center">
        <div class="col-md-6">
            <div class="card">
                <div class="card-header">
                    <h4 class="mb-0">Claim Your Old Account</h4>
                </div>
                <div class="card-body">
                    <p class="text-muted">
                        If you had an account on the old journal system, enter your username below to request access.
                        An administrator will review your request and contact you.
                    </p>

                    @if (session('success'))
                        <div class="alert alert-success">
                            {{ session('success') }}
                        </div>
                    @endif

                    <form method="POST" action="{{ route('claim-account.submit') }}">
                        @csrf

                        <div class="form-group mb-3">
                            <label for="username">Your Old Username</label>
                            <input type="text" id="username" name="username"
                                class="form-control @error('username') is-invalid @enderror"
                                value="{{ old('username') }}" required>
                            @error('username')
                                <span class="invalid-feedback">{{ $message }}</span>
                            @enderror
                        </div>

                        <div class="form-group mb-3">
                            <label for="email">Your Email Address</label>
                            <input type="email" id="email" name="email"
                                class="form-control @error('email') is-invalid @enderror"
                                value="{{ old('email') }}" required
                                placeholder="We'll contact you at this email">
                            @error('email')
                                <span class="invalid-feedback">{{ $message }}</span>
                            @enderror
                        </div>

                        <button type="submit" class="btn btn-primary w-100">
                            Submit Claim Request
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
