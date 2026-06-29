import React from 'react';
import { Box, Typography, Chip, Dialog, DialogTitle, DialogContent, DialogActions, CircularProgress, Button, List, ListItem, ListItemAvatar, ListItemText, Divider } from '@mui/material';
import HistoryIcon from '@mui/icons-material/History';
import { format } from 'date-fns';
import { vi } from 'date-fns/locale';
import NameRenderer from '../Cosmetic/NameRenderer';
import type { GachaHistoryItem } from '../../types/gacha';
import { RARITY_LABELS } from '../../types/cosmetic';

interface HistoryDialogProps {
  open: boolean;
  onClose: () => void;
  historyData: GachaHistoryItem[];
  loading: boolean;
  theme: 'SUMMER' | 'WORLDCUP'; // <-- Thêm Prop theme
}

export default function HistoryDialog({ open, onClose, historyData, loading, theme }: HistoryDialogProps) {
  
  // Cấu hình DNA thiết kế
  const uiConfig = theme === 'SUMMER' 
    ? { color: '#ea580c', bgBadge: '#f97316', radius: '50%' } // Mùa hè: Tròn, Cam
    : { color: '#16a34a', bgBadge: '#15803d', radius: '6px' }; // Bóng đá: Vuông, Xanh lá

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: 1 }}>
        <HistoryIcon sx={{ color: uiConfig.color }} /> Lịch sử quay rương
      </DialogTitle>
      <DialogContent dividers sx={{ p: 0 }}>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}><CircularProgress sx={{ color: uiConfig.color }} /></Box>
        ) : historyData.length === 0 ? (
          <Box sx={{ textAlign: 'center', p: 4, color: 'text.secondary' }}><Typography>Bạn chưa quay rương lần nào.</Typography></Box>
        ) : (
          <List sx={{ width: '100%', bgcolor: 'background.paper' }}>
            {historyData.map((item, index) => (
              <React.Fragment key={item.itemId}>
                <ListItem>
                  <ListItemAvatar>
                    {/* Badge hiển thị Index tuân thủ DNA hình khối */}
                    <Box sx={{ 
                      width: 40, height: 40, 
                      display: 'flex', alignItems: 'center', justifyContent: 'center', 
                      bgcolor: uiConfig.bgBadge, color: 'white', 
                      borderRadius: uiConfig.radius, /* Tròn cho Hè, Vuông cho WC */
                      fontWeight: 'bold', fontSize: '14px',
                      boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
                    }}>
                      {index + 1}
                    </Box>
                  </ListItemAvatar>
                  <ListItemText 
                    primary={
                      <Box display="flex" alignItems="center" gap={1}>
                        {item.type === 'NAME_COLOR' ? (
                          <NameRenderer name={item.name} effectKey={item.effectKey} style={{ fontWeight: 'bold' }} />
                        ) : (
                          <Typography fontWeight="bold">{item.name}</Typography>
                        )}
                      </Box>
                    } 
                    secondary={format(new Date(item.purchasedAt), "HH:mm - dd/MM/yyyy", { locale: vi })} 
                  />
                  <Chip label={RARITY_LABELS[item.rarity]} size="small" sx={{ ml: 2, fontWeight: 'bold' }} />
                </ListItem>
                {index < historyData.length - 1 && <Divider component="li" />}
              </React.Fragment>
            ))}
          </List>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} variant="outlined" sx={{ borderRadius: 50, color: uiConfig.color, borderColor: uiConfig.color }}>
          Đóng
        </Button>
      </DialogActions>
    </Dialog>
  );
}