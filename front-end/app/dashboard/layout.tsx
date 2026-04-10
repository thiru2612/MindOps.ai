"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  LayoutDashboard,
  Key,
  TerminalSquare,
  LineChart,
  LogOut,
  Cloud,
  ChevronRight,
  Activity,
  Layers,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { ThemeProvider } from "@/components/theme-provider";
import { ThemeToggle } from "@/components/theme-toggle";

// ── Types ────────────────────────────────────────────────────────────────────

interface NavItem {
  label: string;
  href: string;
  icon: React.ElementType;
  badge?: string;
}

// ── Constants ────────────────────────────────────────────────────────────────

const NAV_ITEMS: NavItem[] = [
  { label: "Overview", href: "/dashboard", icon: LayoutDashboard },
  { label: "Credential Vault", href: "/dashboard/credentials", icon: Key },
  { label: "AI Orchestrator", href: "/dashboard/orchestrator", icon: TerminalSquare },
  { label: "FinOps Azure", href: "/dashboard/finops", icon: LineChart },
  { label: "Deployments History", href: "/dashboard/deployments", icon: Layers }
];

// ── Sub-components ───────────────────────────────────────────────────────────

function NavLink({ item, isActive }: { item: NavItem; isActive: boolean }) {
  const Icon = item.icon;

  return (
    <Link
      href={item.href}
      className={cn(
        "group relative flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium",
        "transition-all duration-200 ease-out",
        isActive
          ? [
            "bg-cyan-500/10 text-cyan-400",
            "before:absolute before:left-0 before:top-1/2 before:-translate-y-1/2",
            "before:h-5 before:w-0.5 before:rounded-full before:bg-cyan-400",
          ]
          : "text-slate-400 hover:bg-slate-800/60 hover:text-slate-200"
      )}
    >
      <Icon
        size={16}
        className={cn(
          "shrink-0 transition-transform duration-200",
          isActive ? "text-cyan-400" : "group-hover:scale-110"
        )}
      />
      <span className="flex-1 truncate tracking-wide">{item.label}</span>
      {item.badge && (
        <span className="rounded-full bg-cyan-500/20 px-1.5 py-0.5 text-xs text-cyan-400">
          {item.badge}
        </span>
      )}
      {isActive && (
        <ChevronRight size={12} className="text-cyan-400/60" />
      )}
    </Link>
  );
}

function StatusDot() {
  return (
    <span className="relative flex h-2 w-2">
      <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-60" />
      <span className="relative inline-flex h-2 w-2 rounded-full bg-emerald-400" />
    </span>
  );
}

function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const [userEmail, setUserEmail] = useState<string>("");

  useEffect(() => {
    try {
      const raw = localStorage.getItem("mindops_user");
      if (raw) {
        const parsed = JSON.parse(raw);
        setUserEmail(parsed.email ?? "");
      }
    } catch {
      // silently ignore parse errors
    }
  }, []);

  function handleLogout() {
    localStorage.removeItem("jwt_token");
    localStorage.removeItem("mindops_refresh_token");
    localStorage.removeItem("mindops_user");
    // router.push("/login");
    router.push("/");
  }

  // derive initials from email
  const initials = userEmail
    ? userEmail.split("@")[0].slice(0, 2).toUpperCase()
    : "MO";

  return (
    <aside
      className={cn(
        "fixed inset-y-0 left-0 z-40 flex w-64 flex-col",
        "border-r border-slate-800/80 bg-slate-900/50",
        "backdrop-blur-xl"
      )}
    >
      {/* Subtle scanline texture overlay */}
      <div
        className="pointer-events-none absolute inset-0 opacity-[0.03]"
        style={{
          backgroundImage:
            "repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(255,255,255,0.8) 2px, rgba(255,255,255,0.8) 3px)",
          backgroundSize: "100% 3px",
        }}
      />

      {/* ── Logo ─────────────────────────────────────────────────── */}
      <div className="flex items-center gap-3 border-b border-slate-800/80 px-5 py-5">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-cyan-500/15 ring-1 ring-cyan-500/30">
          <Cloud size={16} className="text-cyan-400" />
        </div>
        <div className="leading-none">
          <p className="font-mono text-sm font-semibold tracking-widest text-slate-100">
            MIND<span className="text-cyan-400">OPS</span>
          </p>
          <p className="mt-0.5 font-mono text-[10px] tracking-widest text-slate-500">
            CLOUD PLATFORM
          </p>
        </div>
      </div>

      {/* ── System status strip ───────────────────────────────────── */}
      <div className="flex items-center gap-2 border-b border-slate-800/60 bg-slate-950/30 px-5 py-2">
        <StatusDot />
        <span className="font-mono text-[10px] tracking-widest text-slate-500">
          SYSTEMS NOMINAL
        </span>
        <Activity size={10} className="ml-auto text-slate-600" />
      </div>

      {/* ── Navigation ───────────────────────────────────────────── */}
      <nav className="flex-1 space-y-0.5 overflow-y-auto px-3 py-4">
        <p className="mb-2 px-3 font-mono text-[9px] font-semibold tracking-[0.2em] text-slate-600">
          NAVIGATION
        </p>
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.href}
            item={item}
            isActive={
              item.href === "/dashboard"
                ? pathname === "/dashboard"
                : pathname.startsWith(item.href)
            }
          />
        ))}
      </nav>

      {/* ── User area ─────────────────────────────────────────────── */}
      <div className="border-t border-slate-800/80 p-3">
        <div className="mb-2 flex items-center gap-3 rounded-lg bg-slate-800/40 px-3 py-2.5">
          <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-slate-700 font-mono text-xs font-bold text-slate-300">
            {initials}
          </div>
          <div className="min-w-0 flex-1">
            <p className="truncate font-mono text-xs text-slate-300">
              {userEmail || "operator"}
            </p>
            <p className="font-mono text-[10px] text-slate-600">ROLE_USER</p>
          </div>
        </div>

        <button
          onClick={handleLogout}
          className={cn(
            "group flex w-full items-center gap-3 rounded-lg px-3 py-2.5",
            "text-sm text-slate-500 transition-all duration-200",
            "hover:bg-red-500/10 hover:text-red-400"
          )}
        >
          <LogOut
            size={15}
            className="shrink-0 transition-transform duration-200 group-hover:-translate-x-0.5"
          />
          <span className="font-medium tracking-wide">Log Out</span>
        </button>
      </div>
    </aside>
  );
}

// ── Root Layout ──────────────────────────────────────────────────────────────

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen bg-slate-950">
      {/* Background grid pattern */}
      <div
        className="pointer-events-none fixed inset-0 opacity-[0.04]"
        style={{
          backgroundImage: `
            linear-gradient(rgba(148,163,184,0.3) 1px, transparent 1px),
            linear-gradient(90deg, rgba(148,163,184,0.3) 1px, transparent 1px)
          `,
          backgroundSize: "40px 40px",
        }}
      />

      <Sidebar />

      {/* Main content */}
      <main className="relative min-h-screen pl-64">
        <div className="mx-auto max-w-7xl p-8">{children}</div>
      </main>
    </div>
  );
}