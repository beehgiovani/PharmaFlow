import { Outlet, Link, useLocation } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Package, 
  ShoppingCart, 
  Users, 
  Megaphone, 
  Settings,
  Menu,
  X
} from 'lucide-react';
import { useState } from 'react';
import { cn } from '../lib/utils';

const NAV_ITEMS = [
  { label: 'Dashboard', icon: LayoutDashboard, path: '/admin' },
  { label: 'Orders', icon: ShoppingCart, path: '/admin/orders' },
  { label: 'Products', icon: Package, path: '/admin/products' },
  { label: 'Marketing', icon: Megaphone, path: '/admin/marketing' },
  { label: 'Team', icon: Users, path: '/admin/team' },
  { label: 'Settings', icon: Settings, path: '/admin/settings' },
];

export function AdminLayout() {
  const [isSidebarOpen, setSidebarOpen] = useState(true);
  const location = useLocation();

  return (
    <div className="min-h-screen bg-slate-50 flex font-sans text-slate-900">
      
      {/* Sidebar */}
      <aside 
        className={cn(
          "fixed inset-y-0 left-0 z-40 w-64 bg-white border-r border-slate-200 shadow-xl transition-transform duration-300 ease-in-out lg:translate-x-0 lg:static lg:shadow-none",
          !isSidebarOpen && "-translate-x-full lg:w-0 lg:overflow-hidden lg:border-r-0"
        )}
      >
        <div className="h-16 flex items-center justify-between px-6 border-b border-slate-100">
           <span className="text-lg font-bold bg-gradient-to-tr from-blue-700 to-indigo-600 bg-clip-text text-transparent">
             Admin Panel
           </span>
           <button onClick={() => setSidebarOpen(false)} className="lg:hidden p-1 hover:bg-slate-100 rounded-md">
             <X className="w-5 h-5 text-slate-500"/>
           </button>
        </div>

        <nav className="p-4 space-y-1">
          {NAV_ITEMS.map((item) => {
             const Icon = item.icon;
             const isActive = location.pathname === item.path;
             return (
               <Link
                 key={item.path}
                 to={item.path}
                 className={cn(
                   "flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-200",
                   isActive 
                     ? "bg-blue-50 text-blue-700 shadow-sm ring-1 ring-blue-100" 
                     : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                 )}
               >
                 <Icon className={cn("w-5 h-5", isActive ? "text-blue-600" : "text-slate-400")} />
                 {item.label}
               </Link>
             )
          })}
        </nav>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col min-w-0">
        <header className="h-16 bg-white/80 backdrop-blur-sm border-b border-slate-200 flex items-center px-4 justify-between sticky top-0 z-30">
          <button 
             onClick={() => setSidebarOpen(!isSidebarOpen)}
             className="p-2 hover:bg-slate-100 rounded-lg text-slate-600 transition-colors"
          >
            <Menu className="w-5 h-5" />
          </button>
          
          <div className="flex items-center gap-4">
            <div className="text-right hidden sm:block">
              <div className="text-sm font-medium text-slate-900">Admin User</div>
              <div className="text-xs text-slate-500">Manager</div>
            </div>
            <div className="w-9 h-9 rounded-full bg-slate-200 ring-2 ring-white shadow-sm" />
          </div>
        </header>

        <main className="flex-1 p-6 overflow-y-auto">
          <Outlet />
        </main>
      </div>

    </div>
  );
}
