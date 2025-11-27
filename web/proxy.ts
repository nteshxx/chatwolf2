import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function proxy(request: NextRequest) {
  const token = request.cookies.get('auth-storage')?.value;

  const isAuthPage = request.nextUrl.pathname.startsWith('/auth');
  const isProtectedPage =
    request.nextUrl.pathname.startsWith('/dashboard') ||
    request.nextUrl.pathname.startsWith('/profile');

  // Redirect to login if accessing protected route without token
  if (isProtectedPage && !token) {
    return NextResponse.redirect(new URL('/auth/login', request.url));
  }

  // Redirect to chat if already logged in and trying to access auth pages
  if (isAuthPage && token) {
    return NextResponse.redirect(new URL('/dashboard', request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    '/dashboard/:path*',
    '/profile/:path*',
    '/auth/login',
    '/auth/register',
    '/auth/verify',
  ],
};
