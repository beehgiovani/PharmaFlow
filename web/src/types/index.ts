export interface Product {
    id: string;
    storeId: string;
    name: string;
    ean: string;
    internalCode: string; // "789..." etc
    description: string;
    price: number;
    promoPrice: number;
    imageUrl: string;
    category: string;
    stockQuantity: number;
    expirationDate: number; // Timestamp in milliseconds
    isFeatured: boolean;
    isActive: boolean;
    searchIndex: string;
}

export interface Store {
    id: string;
    name: string;
    whatsappNumber: string;
    address: string;
    pixKey: string;
    deliveryFee: number;
    logoUrl: string;
    active: boolean;
    openingTime: string; // "08:00"
    closingTime: string; // "22:00"
    deliveryStart: string;
    deliveryEnd: string;
}

export interface Coupon {
    id: string;
    code: string;
    type: 'PERCENT' | 'FIXED' | string;
    value: number;
    minPurchase: number;
    isActive: boolean;
    storeId: string; // Empty string for Global
}

export interface Banner {
    id: string;
    imageUrl: string;
    storeId: string;
    active: boolean;
    order: number;
}

export interface User {
    id: string;
    name: string;
    email: string;
    whatsapp: string;
    neighborhood: string;
    address: string;
    authorizedContact: boolean;
    role: 'client' | 'admin' | 'driver' | 'manager' | string;
    active: boolean;
    driverZone: string;
    assignedStoreId: string;
    points: number;
}

export interface CartItemSummary {
    name: string;
    quantity: number;
    price: number;
    imageUrl: string;
}

export interface Order {
    firestoreId: string; // Unlike Android room localId, we rely on firestore Id
    userId: string;
    storeId: string;
    storeName: string;
    
    totalAmount: number;
    subtotalAmount: number;
    discountAmount: number;
    deliveryFee: number;
    pointsUsed: number;
    
    paymentMethod: string;
    isDelivery: boolean;
    deliveryAddress: string;
    addressNeighborhood: string;
    addressComplement: string;
    
    date: number; // Timestamp
    status: 'Pendente' | 'Aceito' | 'Saiu para Entrega' | 'Entregue' | 'Cancelado' | string;
    
    customerName: string;
    customerPhone: string;
    
    // In Android this is a JSON string 'itemsJson', but in pure JS/TS we might try to keep it as object if possible, 
    // strictly adhering to Android might mean keeping it as string or handling the conversion. 
    // If the Firestore document stores it as a string, we keep it as string.
    // If Firestore stores it as an array (supported), we use CartItemSummary[].
    // *Assumption*: Android uses Gson to serialize to string for Room, but might save to Firestore as array/map? 
    // Checking Android Order.kt, it's a @Entity for Room. The Firestore implementation usually maps lists to arrays.
    // Let's assume Firestore stores it as an array of objects for better query capabilities, 
    // but if the Android app literally blindly saves the JSON string to Firestore, we must conform.
    // Let's support both for now or stick to a deserialized version in the type.
    items: CartItemSummary[]; 
    
    cancellationReason: string;
    driverId: string;
    driverName: string;
}
