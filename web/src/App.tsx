import ProductListPage from './features/admin/products/ProductListPage';
import AddProductPage from './features/admin/products/AddProductPage';
import CartPage from './features/client/cart/CartPage';
import CheckoutPage from './features/client/cart/CheckoutPage';
import AdminOrdersPage from './features/admin/orders/AdminOrdersPage';

import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ClientLayout } from './layouts/ClientLayout';
import { AdminLayout } from './layouts/AdminLayout';
import HomePage from './features/client/HomePage';
import DashboardPage from './features/admin/DashboardPage';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Client Routes */}
        <Route path="/" element={<ClientLayout />}>
          <Route index element={<HomePage />} />
          <Route path="cart" element={<CartPage />} />
          <Route path="checkout" element={<CheckoutPage />} />
        </Route>

        {/* Admin Routes */}
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="orders" element={<AdminOrdersPage />} />
          
          {/* Product Management */}
          <Route path="products" element={<ProductListPage />} />
          <Route path="products/new" element={<AddProductPage />} />
          
          <Route path="marketing" element={<div className="p-4">Marketing (Coming Soon)</div>} />
          <Route path="team" element={<div className="p-4">Team (Coming Soon)</div>} />
          <Route path="settings" element={<div className="p-4">Settings (Coming Soon)</div>} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
