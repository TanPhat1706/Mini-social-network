import React, { useState, useRef } from 'react';
import { Tooltip, Typography, Box } from '@mui/material';
import api from '../../api/api';

interface ReactionTooltipProps {
  postId: number;
  totalReactions: number;
  children: React.ReactElement;
}

export default function ReactionTooltip({ postId, totalReactions, children }: ReactionTooltipProps) {
  const [open, setOpen] = useState(false);
  const [isFetched, setIsFetched] = useState(false);
  
  const [tooltipContent, setTooltipContent] = useState<React.ReactNode>(
    <Typography variant="caption">Đang tải...</Typography>
  );
  
  const fetchTimeoutRef = useRef<number | null>(null);

  const handleMouseEnter = () => {
    setOpen(true);

    if (isFetched) return;

    if (!isFetched) {
      setTooltipContent(<Typography variant="caption">Đang tải...</Typography>);
    }

    fetchTimeoutRef.current = window.setTimeout(async () => {
      try {
        const response = await api.get(`/api/posts/${postId}/reactions?page=0&size=5`);
        const users = response.data.content;

        if (users.length > 0) {
          const contentLayout = (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5, py: 0.5 }}>
              {users.map((u: any, index: number) => (
                <Typography key={index} variant="caption" >
                  {u.fullName}
                </Typography>
              ))}
              
              {totalReactions > users.length && (
                <Typography variant="caption" sx={{ fontStyle: 'italic', opacity: 0.8, mt: 0.5 }}>
                  và {totalReactions - users.length} người khác...
                </Typography>
              )}
            </Box>
          );
          
          setTooltipContent(contentLayout);
          setIsFetched(true);
        } else {
          setTooltipContent(<Typography variant="caption">Chưa có tương tác</Typography>);
        }
      } catch (error) {
        setTooltipContent(<Typography variant="caption">Không thể tải dữ liệu</Typography>);
      }
    }, 400); 
  };

  const handleMouseLeave = () => {
    setOpen(false);
    if (fetchTimeoutRef.current) {
      window.clearTimeout(fetchTimeoutRef.current);
    }
  };

  return (
    <Tooltip
      open={open}
      title={tooltipContent}
      placement="top"
      arrow
      enterDelay={200} 
      leaveDelay={0}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    >
      <span style={{ display: 'inline-flex' }}>
        {children}
      </span>
    </Tooltip>
  );
}