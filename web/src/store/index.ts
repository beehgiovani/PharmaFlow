import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { CartItemSummary, Product, Store, User } from '../types';

interface CartItem extends CartItemSummary {
    productId: string;
}

interface AppState {
    // Session
    user: User | null;
    selectedStore: Store | null;
    
    // Cart
    cart: CartItem[];
    addToCart: (product: Product) => void;
    removeFromCart: (productId: string) => void;
    updateQuantity: (productId: string, quantity: number) => void;
    clearCart: () => void;
    
    // Actions
    setUser: (user: User | null) => void;
    setSelectedStore: (store: Store | null) => void;
}

export const useAppStore = create<AppState>()(
    persist(
        (set, get) => ({
            user: null,
            selectedStore: null,
            cart: [],
            
            setUser: (user) => set({ user }),
            setSelectedStore: (store) => set({ selectedStore: store }),
            
            addToCart: (product) => {
                const { cart } = get();
                const existing = cart.find(item => item.productId === product.id);
                
                if (existing) {
                    set({
                        cart: cart.map(item => 
                            item.productId === product.id 
                                ? { ...item, quantity: item.quantity + 1 }
                                : item
                        )
                    });
                } else {
                    set({
                        cart: [...cart, {
                            productId: product.id,
                            name: product.name,
                            price: product.promoPrice > 0 ? product.promoPrice : product.price,
                            imageUrl: product.imageUrl,
                            quantity: 1
                        }]
                    });
                }
            },
            
            removeFromCart: (productId) => {
                set({ cart: get().cart.filter(item => item.productId !== productId) });
            },
            
            updateQuantity: (productId, quantity) => {
                if (quantity <= 0) {
                    get().removeFromCart(productId);
                    return;
                }
                set({
                    cart: get().cart.map(item =>
                        item.productId === productId ? { ...item, quantity } : item
                    )
                });
            },
            
            clearCart: () => set({ cart: [] })
        }),
        {
            name: 'pharmaflow-storage',
            partialize: (state) => ({ 
                user: state.user, 
                selectedStore: state.selectedStore,
                cart: state.cart 
            }),
        }
    )
);
