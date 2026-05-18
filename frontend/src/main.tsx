import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.tsx';
import './index.css';

import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { BrowserRouter } from 'react-router-dom';

// 🟢 IMPORT 2 THỨ MỚI TỪ FILE THEME
import { ColorModeContext, useThemeConfig } from './styles/theme.ts';

// 🟢 TẠO COMPONENT BỌC THEME ĐỂ DÙNG ĐƯỢC HOOK
const ThemeWrapper = ({ children }: { children: React.ReactNode }) => {
  const { theme, toggleColorMode, mode } = useThemeConfig();

  return (
    <ColorModeContext.Provider value={{ toggleColorMode, mode }}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </ThemeProvider>
    </ColorModeContext.Provider>
  );
};

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ThemeWrapper>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </ThemeWrapper>
  </React.StrictMode>
);