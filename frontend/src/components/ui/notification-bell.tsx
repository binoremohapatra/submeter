"use client";

import { useEffect, useState, useRef } from "react";
import { useSession } from "next-auth/react";
import { apiFetch } from "@/lib/api";
import { motion, AnimatePresence } from "framer-motion";
import { Bell, Check, ExternalLink } from "lucide-react";
import Link from "next/link";
import { EASE_OUT_EXPO } from "@/lib/motion";
import { formatDistanceToNow } from "date-fns";

interface Notification {
  id: string;
  type: string;
  title: string;
  body: string;
  link?: string;
  readAt: string | null;
  createdAt: string;
}

export function NotificationBell() {
  const { data: session } = useSession();
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const fetchNotifications = async () => {
    if (!session) return;
    try {
      const data = await apiFetch<{ notifications: Notification[], unreadCount: number }>("/notifications", session);
      setNotifications(data.notifications || []);
      setUnreadCount(data.unreadCount || 0);
    } catch (e) {
      console.error("Failed to fetch notifications", e);
    }
  };

  useEffect(() => {
    fetchNotifications();

    const interval = setInterval(() => {
      if (!document.hidden) {
        fetchNotifications();
      }
    }, 30000);

    const handleVisibilityChange = () => {
      if (!document.hidden) {
        fetchNotifications();
      }
    };

    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      clearInterval(interval);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [session]);

  // Close on outside click
  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    if (open) {
      document.addEventListener("mousedown", handleClick);
    }
    return () => document.removeEventListener("mousedown", handleClick);
  }, [open]);

  const markAllRead = async () => {
    if (!session || unreadCount === 0) return;
    try {
      await apiFetch("/notifications/mark-read", session, { method: "POST" });
      setUnreadCount(0);
      setNotifications(prev => prev.map(n => ({ ...n, readAt: n.readAt || new Date().toISOString() })));
    } catch (e) {
      console.error("Failed to mark notifications as read", e);
    }
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setOpen(!open)}
        className="relative flex items-center justify-center p-2 rounded-md hover:bg-white/5 transition-colors text-neutral-400 hover:text-neutral-100"
        aria-label={`Notifications ${unreadCount > 0 ? `(${unreadCount} unread)` : ""}`}
      >
        <Bell className="w-[18px] h-[18px]" />
        {unreadCount > 0 && (
          <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-emerald-500 rounded-full ring-2 ring-neutral-950" />
        )}
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: -4, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, scale: 0.96 }}
            transition={{ duration: 0.15, ease: EASE_OUT_EXPO }}
            className="absolute right-0 top-full mt-2 w-80 bg-neutral-900 border border-neutral-800 rounded-xl shadow-2xl z-50 overflow-hidden"
          >
            <div className="flex items-center justify-between px-4 py-3 border-b border-neutral-800 bg-neutral-900/50">
              <h3 className="text-sm font-semibold text-neutral-100">Notifications</h3>
              {unreadCount > 0 && (
                <button
                  onClick={markAllRead}
                  className="text-xs font-medium text-emerald-500 hover:text-emerald-400 flex items-center gap-1 transition-colors"
                >
                  <Check className="w-3.5 h-3.5" />
                  Mark all read
                </button>
              )}
            </div>

            <div className="max-h-[320px] overflow-y-auto">
              {notifications.length === 0 ? (
                <div className="px-4 py-8 text-center text-sm text-neutral-500">
                  No notifications yet.
                </div>
              ) : (
                <div className="flex flex-col">
                  {notifications.map((n) => (
                    <div
                      key={n.id}
                      className={`relative px-4 py-3 border-b border-neutral-800/50 last:border-0 hover:bg-neutral-800/30 transition-colors ${!n.readAt ? "bg-neutral-800/10" : ""}`}
                    >
                      {!n.readAt && (
                        <div className="absolute left-0 top-0 bottom-0 w-0.5 bg-emerald-500" />
                      )}
                      <div className="flex items-start justify-between gap-3">
                        <div className="flex-1 min-w-0">
                          <p className={`text-sm font-medium truncate ${!n.readAt ? "text-neutral-100" : "text-neutral-300"}`}>
                            {n.title}
                          </p>
                          <p className="text-xs text-neutral-500 mt-1 line-clamp-2 leading-relaxed">
                            {n.body}
                          </p>
                          <p className="text-[10px] text-neutral-600 mt-2 font-medium tracking-wide uppercase">
                            {formatDistanceToNow(new Date(n.createdAt), { addSuffix: true })}
                          </p>
                        </div>
                        {n.link && (
                          <Link href={n.link} onClick={() => setOpen(false)} className="flex-shrink-0 mt-0.5 text-neutral-500 hover:text-neutral-300 transition-colors">
                            <ExternalLink className="w-4 h-4" />
                          </Link>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
