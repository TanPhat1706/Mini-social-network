import React, { useState, useEffect } from 'react';
import { 
  Box, Container, Typography, Tab, Tabs, Card, CardContent, 
  Button, Snackbar, Alert, CircularProgress, Chip 
} from '@mui/material';
import DiamondIcon from '@mui/icons-material/Diamond';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import api from '../api/api';
import type { User } from '../types';

import AvatarWithFrame from '../components/AvatarWithFrame'; 
import ColoredName from '../components/ColoredName'; 

interface ShopItem {
  id: number;
  name: string;
  type: string;
  imageUrl: string; 
  price: number;
  description: string;
}

export default function ShopPage() {
  const [tabValue, setTabValue] = useState(0);
  const [user, setUser] = useState<User | null>(null);
  
  const [shopItems, setShopItems] = useState<ShopItem[]>([]);
  const [inventory, setInventory] = useState<ShopItem[]>([]);
  const [loading, setLoading] = useState(true);

  const [toast, setToast] = useState({ open: false, message: '', type: 'success' as 'success' | 'error' | 'info' });

  const fetchData = async () => {
    try {
      setLoading(true);
      const [userRes, itemsRes, invRes] = await Promise.all([
        api.get('/api/auth/profile'),
        api.get('/api/shop/items'),          
        api.get('/api/shop/items/inventory') 
      ]);
      setUser(userRes.data);
      setShopItems(itemsRes.data);
      setInventory(invRes.data);
    } catch (error: any) {
      showToast(error.response?.data?.error || "Lỗi tải dữ liệu cửa hàng", 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const showToast = (message: string, type: 'success' | 'error' | 'info') => setToast({ open: true, message, type });

  const handleBuy = async (itemId: number) => {
    try {
      const res = await api.post(`/api/shop/items/${itemId}/buy`);
      showToast(res.data.message || "Mua vật phẩm thành công!", 'success');
      fetchData(); 
    } catch (error: any) {
      showToast(error.response?.data?.error || "Không đủ điểm hoặc lỗi hệ thống", 'error');
    }
  };

  const handleEquip = async (itemId: number) => {
    try {
      const res = await api.put(`/api/shop/items/${itemId}/equip`);
      showToast(res.data.message || "Đã trang bị thành công!", 'success');
      fetchData(); 
    } catch (error: any) {
      showToast(error.response?.data?.error || "Lỗi trang bị", 'error');
    }
  };

  // 🟢 HÀM MỚI: XỬ LÝ THÁO TRANG BỊ
  const handleUnequip = async (itemId: number) => {
    try {
      // 🛑 CHÚ Ý: Đường dẫn API này đang là giả định, chờ bạn cung cấp Controller để fix lại cho chuẩn 100%
      const res = await api.put(`/api/shop/items/unequip`); 
      showToast(res.data.message || "Đã tháo trang bị!", 'success');
      fetchData(); 
    } catch (error: any) {
      showToast(error.response?.data?.error || "Lỗi khi tháo trang bị", 'error');
    }
  };

  const isOwned = (itemId: number) => inventory.some(item => item.id === itemId);

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4, background: 'linear-gradient(135deg, #1f2937, #111827)', p: 3, borderRadius: '16px', color: 'white', boxShadow: '0 10px 25px rgba(0,0,0,0.2)' }}>
        <Box>
          <Typography variant="h4" fontWeight="bold">Ngân Hàng Đổi Điểm</Typography>
          <Typography variant="body1" color="gray">Dùng VPTL Points để thể hiện đẳng cấp!</Typography>
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, background: 'rgba(255,255,255,0.1)', px: 3, py: 1.5, borderRadius: '50px' }}>
          <DiamondIcon sx={{ color: '#F5A623', fontSize: 32 }} />
          <Typography variant="h5" fontWeight="bold" color="#F5A623">{user?.vptlPoints || 0}</Typography>
        </Box>
      </Box>

      <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)} centered sx={{ mb: 4 }}>
        <Tab label="Cửa Hàng" sx={{ fontWeight: 'bold', fontSize: '16px' }} />
        <Tab label={`Tủ Đồ Của Tôi (${inventory.length})`} sx={{ fontWeight: 'bold', fontSize: '16px' }} />
      </Tabs>

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 10 }}><CircularProgress /></Box>
      ) : (
        <Box sx={{ 
            display: 'grid', 
            gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(3, 1fr)', lg: 'repeat(4, 1fr)' }, 
            gap: 4 
        }}>
          {(tabValue === 0 ? shopItems : inventory).map((item) => {
            const owned = isOwned(item.id);
            
            const isEquipped = item.type === 'NAME_COLOR' 
                ? (user as any)?.currentNameColor === item.imageUrl 
                : (user as any)?.currentAvatarFrame === item.imageUrl;

            return (
                <Card key={item.id} sx={{ borderRadius: '16px', transition: '0.3s', '&:hover': { transform: 'translateY(-5px)', boxShadow: '0 10px 20px rgba(0,0,0,0.1)' } }}>
                  <Box sx={{ height: '180px', display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', backgroundColor: '#f3f4f6', position: 'relative' }}>
                    
                    {item.type === 'NAME_COLOR' ? (
                        <>
                            <AvatarWithFrame src={user?.avatarUrl} name={user?.fullName} size={60} />
                            <Typography variant="h6" sx={{ mt: 1.5, fontWeight: 'bold' }}>
                                <ColoredName name={user?.fullName?.split(' ').pop() || 'Tên'} colorClass={item.imageUrl} />
                            </Typography>
                        </>
                    ) : (
                        <AvatarWithFrame src={user?.avatarUrl} name={user?.fullName} frameClass={item.imageUrl} size={80} />
                    )}

                    {isEquipped && (
                       <Chip icon={<CheckCircleIcon />} label="Đang dùng" color="success" size="small" sx={{ position: 'absolute', top: 10, right: 10, fontWeight: 'bold' }} />
                    )}
                  </Box>

                  <CardContent sx={{ textAlign: 'center' }}>
                    <Typography variant="h6" fontWeight="bold" noWrap>{item.name}</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2, height: '40px', overflow: 'hidden' }}>{item.description}</Typography>
                    
                    {tabValue === 0 ? (
                      owned ? (
                        <Button fullWidth variant="contained" disabled sx={{ borderRadius: '50px', fontWeight: 'bold' }}>Đã Sở Hữu</Button>
                      ) : (
                        <Button fullWidth variant="contained" color="warning" onClick={() => handleBuy(item.id)} sx={{ borderRadius: '50px', fontWeight: 'bold', display: 'flex', gap: 1 }}>
                          Mua ngay - {item.price} <DiamondIcon fontSize="small" />
                        </Button>
                      )
                    ) : (
                      /* 🟢 UPDATE: Đổi nút Đã Trang Bị (disabled) thành nút Tháo Trang Bị (Clickable, màu đỏ) */
                      isEquipped ? (
                        <Button fullWidth variant="outlined" color="error" onClick={() => handleUnequip(item.id)} sx={{ borderRadius: '50px', fontWeight: 'bold' }}>Tháo Trang Bị</Button>
                      ) : (
                        <Button fullWidth variant="contained" color="primary" onClick={() => handleEquip(item.id)} sx={{ borderRadius: '50px', fontWeight: 'bold' }}>Trang Bị Lên Người</Button>
                      )
                    )}
                  </CardContent>
                </Card>
            );
          })}
          
          {(tabValue === 0 ? shopItems : inventory).length === 0 && (
              <Box sx={{ gridColumn: '1 / -1', textAlign: 'center', py: 10, color: 'gray' }}>
                  <Typography variant="h6">Chưa có vật phẩm nào ở đây cả!</Typography>
              </Box>
          )}
        </Box>
      )}

      <Snackbar open={toast.open} autoHideDuration={3000} onClose={() => setToast({ ...toast, open: false })} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        <Alert onClose={() => setToast({ ...toast, open: false })} severity={toast.type} sx={{ width: '100%', fontWeight: 'bold', fontSize: '15px' }}>
          {toast.message}
        </Alert>
      </Snackbar>
    </Container>
  );
}