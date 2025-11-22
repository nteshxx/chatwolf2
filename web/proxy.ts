import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function proxy(request: NextRequest) {
  const token = request.cookies.get('token');
  const isAuthPage =
    request.nextUrl.pathname.startsWith('/login') ||
    request.nextUrl.pathname.startsWith('/register');
  const isProtectedPage =
    request.nextUrl.pathname.startsWith('/chat') ||
    request.nextUrl.pathname.startsWith('/profile');

  // Redirect to login if accessing protected route without token
  if (isProtectedPage && !token) {
    return NextResponse.redirect(new URL('/login', request.url));
  }

  // Redirect to chat if already logged in and trying to access auth pages
  if (isAuthPage && token) {
    return NextResponse.redirect(new URL('/chat', request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/chat/:path*', '/profile/:path*', '/login', '/register'],
};
