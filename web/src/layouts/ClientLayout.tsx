import { Outlet } from 'react-router-dom';
import { ShoppingCart, User, Menu } from 'lucide-react';

export function ClientLayout() {
  return (
    <div className="min-h-screen bg-neutral-50 flex flex-col font-sans text-neutral-900">
      {/* Navbar */}
      <header className="sticky top-0 z-50 backdrop-blur-md bg-white/70 border-b border-white/20 shadow-sm">
        <div className="container mx-auto px-4 h-16 flex items-center justify-between">
          
          <div className="flex items-center gap-2">
            <div className="md:hidden">
              <button className="p-2 hover:bg-black/5 rounded-lg active:scale-95 transition-all">
                <Menu className="w-6 h-6 text-neutral-600" />
              </button>
            </div>
            <h1 className="text-xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">
              PharmaFlow
            </h1>
          </div>

          <div className="flex items-center gap-4">
             <button className="p-2 relative hover:bg-black/5 rounded-full transition-all group">
                <ShoppingCart className="w-6 h-6 text-neutral-600 group-hover:text-blue-600 transition-colors" />
                <span className="absolute top-1 right-1 bg-red-500 text-white text-[10px] font-bold px-1 rounded-full shadow-sm">3</span>
             </button>
             <button className="p-2 hover:bg-black/5 rounded-full transition-all group">
                <User className="w-6 h-6 text-neutral-600 group-hover:text-blue-600 transition-colors" />
             </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 container mx-auto px-4 py-6">
        <Outlet />
      </main>
      
      {/* Footer */}
      <footer className="bg-white border-t border-neutral-200 py-8 text-center text-sm text-neutral-500">
        <p>&copy; {new Date().getFullYear()} PharmaFlow. All rights reserved.</p>
      </footer>
    </div>
  );
}
