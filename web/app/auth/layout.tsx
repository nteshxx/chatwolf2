import Image from 'next/image';

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen bg-black flex">
      {/* Left Side - Branding */}
      <div className="hidden lg:flex lg:w-1/2 bg-linear-to-br from-gray-900 to-black items-center justify-center p-12">
        <div className="text-center">
          <Image
            src="/logo.svg"
            alt="ChatWolf Logo"
            width={200}
            height={200}
            className="mx-auto mb-8"
          />
          <h1 className="text-4xl font-bold text-white mb-4">ChatWolf</h1>
          <p className="text-gray-400 text-lg">
            Connect, Chat, and Collaborate
          </p>
        </div>
      </div>

      {/* Right Side - Auth Forms */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8">
        <div className="w-full max-w-md">
          {/* Mobile Logo */}
          <div className="lg:hidden text-center mb-8">
            <Image
              src="/logo.svg"
              alt="ChatWolf Logo"
              width={80}
              height={80}
              className="mx-auto mb-4"
            />
            <h2 className="text-2xl font-bold text-white">ChatWolf</h2>
          </div>

          {children}
        </div>
      </div>
    </div>
  );
}
