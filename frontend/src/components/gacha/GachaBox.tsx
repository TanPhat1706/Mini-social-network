import React, { useState, useEffect } from 'react';
import { Box, IconButton, Tab, Tabs } from '@mui/material';
import HelpOutlineIcon from '@mui/icons-material/HelpOutline';
import HistoryIcon from '@mui/icons-material/History';

import api from '../../api/api';
import type { CosmeticItem } from '../../types/cosmetic';
import type { GachaPhase, GachaInfoResponse, GachaHistoryItem } from '../../types/gacha';

import PreviewCombos from './PreviewCombos';
import SpinButton from './SpinButton';
import RevealModal from './RevealModal';
import InfoDialog from './InfoDialog';
import HistoryDialog from './HistoryDialog';

import '../../styles/gacha.css';
import EventBanner from '../banner/EventBanner'; // Cẩn thận đường dẫn viết hoa chữ B nếu hệ điều hành phân biệt

interface GachaBoxProps {
  onSuccess?: (wonItem: CosmeticItem, remainingPoints: number) => void;
  userAvatarUrl?: string;
  userName?: string;
}

export default function GachaBox({ onSuccess, userAvatarUrl, userName = 'Bạn' }: GachaBoxProps) {
  // States
  const [currentTheme, setCurrentTheme] = useState<'SUMMER' | 'WORLDCUP'>('SUMMER');
  const [phase, setPhase] = useState<GachaPhase>('IDLE');
  const [wonItem, setWonItem] = useState<CosmeticItem | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [infoOpen, setInfoOpen] = useState(false);
  const [historyOpen, setHistoryOpen] = useState(false);
  const [gachaInfo, setGachaInfo] = useState<GachaInfoResponse | null>(null);
  const [gachaHistory, setGachaHistory] = useState<GachaHistoryItem[]>([]);
  const [loadingHistory, setLoadingHistory] = useState(false);

  // APIs
  const fetchGachaInfo = async () => {
    try {
      const res = await api.get('/api/gacha/info', { params: { theme: currentTheme } });
      setGachaInfo(res.data);
    } catch (error) {
      console.error("Lỗi khi lấy thông tin Gacha", error);
    }
  };

  useEffect(() => { fetchGachaInfo(); }, [currentTheme]);

  const handleOpenHistory = async () => {
    setHistoryOpen(true);
    setLoadingHistory(true);
    try {
      const res = await api.get('/api/gacha/history', { params: { theme: currentTheme } });
      setGachaHistory(res.data);
    } catch (error) {
      console.error("Lỗi khi lấy lịch sử", error);
    } finally {
      setLoadingHistory(false);
    }
  };

  const handleSpin = async () => {
    if (phase !== 'IDLE') return;
    setPhase('FETCHING');
    setErrorMsg(null);
    const minimumSpinTime = new Promise(resolve => setTimeout(resolve, 1000));

    try {
      const [res] = await Promise.all([
        // Đã FIX lỗi gọi nhầm rương SUMMER khi đang ở tab WORLDCUP
        api.post('/api/gacha/spin', null, { params: { theme: currentTheme } }),
        minimumSpinTime
      ]);

      setWonItem(res.data.wonItem);
      setPhase('ANIMATING');

      setTimeout(() => {
        setPhase('REVEAL');
        if (onSuccess) onSuccess(res.data.wonItem, res.data.remainingPoints);
        fetchGachaInfo();
      }, 1500);

    } catch (error: any) {
      setErrorMsg(error.response?.data?.error || 'Có lỗi xảy ra khi mở rương!');
      setPhase('IDLE');
    }
  };

  // Cấu hình DNA cho Box Tổng
  const boxConfig = currentTheme === 'SUMMER'
    ? {
        bg: 'linear-gradient(135deg, #0f172a 0%, #1e3a8a 50%, #0f172a 100%)',
        border: '2px solid rgba(56, 189, 248, 0.3)',
        decor: 'radial-gradient(circle, rgba(56,189,248,0.15) 0%, transparent 60%)'
      }
    : {
        bg: 'linear-gradient(135deg, #022c22 0%, #064e3b 50%, #020617 100%)', // Tông xanh lá cỏ / Đen
        border: '2px solid rgba(74, 222, 128, 0.3)',
        decor: 'radial-gradient(circle, rgba(74,222,128,0.1) 0%, transparent 60%)'
      };

  return (
    <Box sx={{
      position: 'relative', overflow: 'hidden', borderRadius: 4, color: 'white', pt: 6, pb: 4, px: { xs: 2, sm: 4 },
      boxShadow: '0 20px 40px rgba(0,0,0,0.6)',
      background: boxConfig.bg,
      border: boxConfig.border,
      transition: 'background 0.5s ease, border 0.5s ease' // Đổi tab sẽ có hiệu ứng mượt
    }}>

      {/* Background Decor */}
      <Box sx={{ position: 'absolute', top: '-20%', left: '-10%', width: '140%', height: '140%', background: boxConfig.decor, pointerEvents: 'none', transition: 'background 0.5s ease' }} />

      {/* =======================================================
          NÚT TIỆN ÍCH: LỊCH SỬ & THÔNG TIN
          ======================================================= */}
      <Box sx={{ position: 'absolute', top: 16, right: 16, display: 'flex', gap: 1, zIndex: 50 }}>
        <IconButton 
          sx={{ 
            bgcolor: 'rgba(255,255,255,0.1)', color: 'white', 
            backdropFilter: 'blur(4px)',
            '&:hover': { bgcolor: 'rgba(255,255,255,0.2)' } 
          }} 
          onClick={handleOpenHistory}
          title="Lịch sử quay"
        >
          <HistoryIcon />
        </IconButton>
        
        <IconButton 
          sx={{ 
            bgcolor: 'rgba(255,255,255,0.1)', color: 'white', 
            backdropFilter: 'blur(4px)',
            '&:hover': { bgcolor: 'rgba(255,255,255,0.2)' } 
          }} 
          onClick={() => setInfoOpen(true)}
          title="Thông tin tỷ lệ"
        >
          <HelpOutlineIcon />
        </IconButton>
      </Box>

      {/* GIAO DIỆN CHUYỂN TAB SỰ KIỆN NẰM TRÊN CÙNG */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3, display: 'flex', justifyContent: 'center', position: 'relative', zIndex: 10 }}>
        <Tabs
          value={currentTheme}
          onChange={(e, newValue) => setCurrentTheme(newValue)}
          textColor="inherit"
          TabIndicatorProps={{ style: { backgroundColor: currentTheme === 'WORLDCUP' ? '#4ade80' : '#fbbf24', height: '3px', borderRadius: '3px' } }}
        >
          <Tab value="SUMMER" label="🏖️ MÙA HÈ" sx={{ fontWeight: 'bold', fontSize: '16px', opacity: currentTheme === 'SUMMER' ? 1 : 0.6 }} />
          <Tab value="WORLDCUP" label="⚽ WORLD CUP" sx={{ fontWeight: 'bold', fontSize: '16px', opacity: currentTheme === 'WORLDCUP' ? 1 : 0.6 }} />
        </Tabs>
      </Box>

      <EventBanner theme={currentTheme} ownedCount={gachaInfo?.ownedCount || 0} totalPoolSize={gachaInfo?.totalPoolSize || 60} />

      <PreviewCombos phase={phase} userAvatarUrl={userAvatarUrl} userName={userName} theme={currentTheme} />

      <SpinButton phase={phase} errorMsg={errorMsg} gachaInfo={gachaInfo} theme={currentTheme} onSpin={handleSpin} />

      <RevealModal phase={phase} wonItem={wonItem} userAvatarUrl={userAvatarUrl} userName={userName} onClose={() => { setPhase('IDLE'); setWonItem(null); }} />
      <InfoDialog open={infoOpen} onClose={() => setInfoOpen(false)} gachaInfo={gachaInfo} theme={currentTheme} />
      <HistoryDialog open={historyOpen} onClose={() => setHistoryOpen(false)} historyData={gachaHistory} loading={loadingHistory} theme={currentTheme} />
    </Box>
  );
}