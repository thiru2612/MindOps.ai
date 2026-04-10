// "use client";

// import { useState, useEffect, useCallback } from "react";
// import {
//   Key,
//   Plus,
//   Cloud,
//   Server,
//   Trash2,
//   RefreshCw,
//   ShieldCheck,
//   AlertCircle,
//   Copy,
//   Check,
//   ChevronDown,
//   Loader2,
//   Lock,
//   Eye,
//   EyeOff,
// } from "lucide-react";
// import { cn } from "@/lib/utils";
// import api from "@/lib/api";

// // ── Shadcn UI imports ────────────────────────────────────────────────────────
// // Adjust these paths to match your shadcn setup
// import {
//   Dialog,
//   DialogContent,
//   DialogHeader,
//   DialogTitle,
//   DialogDescription,
// } from "@/components/ui/dialog";
// import { Button } from "@/components/ui/button";
// import { Input } from "@/components/ui/input";
// import { Label } from "@/components/ui/label";
// import { Badge } from "@/components/ui/badge";
// import {
//   Select,
//   SelectContent,
//   SelectItem,
//   SelectTrigger,
//   SelectValue,
// } from "@/components/ui/select";
// import { useToast } from "@/components/ui/use-toast";
// import { Toaster } from "@/components/ui/toaster";

// // ── Types ────────────────────────────────────────────────────────────────────

// type Provider = "AWS" | "AZURE";

// interface Credential {
//   credentialId: string;
//   provider: Provider;
//   credentialLabel: string;
//   validationStatus: "VALID" | "INVALID" | "UNVERIFIED";
//   createdAt: string;
//   // AWS
//   accessKeyIdMasked?: string;
//   defaultRegion?: string;
//   // Azure
//   clientIdMasked?: string;
//   subscriptionIdMasked?: string;
// }

// interface AwsFormData {
//   credentialLabel: string;
//   accessKeyId: string;
//   secretAccessKey: string;
//   defaultRegion: string;
// }

// interface AzureFormData {
//   credentialLabel: string;
//   tenantId: string;
//   clientId: string;
//   clientSecret: string;
//   subscriptionId: string;
// }

// // ── Constants ────────────────────────────────────────────────────────────────

// const AWS_REGIONS = [
//   { value: "us-east-1", label: "US East (N. Virginia)" },
//   { value: "us-east-2", label: "US East (Ohio)" },
//   { value: "us-west-1", label: "US West (N. California)" },
//   { value: "us-west-2", label: "US West (Oregon)" },
//   { value: "ap-south-1", label: "Asia Pacific (Mumbai)" },
//   { value: "ap-southeast-1", label: "Asia Pacific (Singapore)" },
//   { value: "ap-southeast-2", label: "Asia Pacific (Sydney)" },
//   { value: "ap-northeast-1", label: "Asia Pacific (Tokyo)" },
//   { value: "eu-west-1", label: "Europe (Ireland)" },
//   { value: "eu-west-2", label: "Europe (London)" },
//   { value: "eu-central-1", label: "Europe (Frankfurt)" },
// ];

// const INITIAL_AWS: AwsFormData = {
//   credentialLabel: "",
//   accessKeyId: "",
//   secretAccessKey: "",
//   defaultRegion: "us-east-1",
// };

// const INITIAL_AZURE: AzureFormData = {
//   credentialLabel: "",
//   tenantId: "",
//   clientId: "",
//   clientSecret: "",
//   subscriptionId: "",
// };

// // ── Helper sub-components ────────────────────────────────────────────────────

// function ProviderIcon({
//   provider,
//   size = 18,
//   className,
// }: {
//   provider: Provider;
//   size?: number;
//   className?: string;
// }) {
//   if (provider === "AWS") {
//     return (
//       <svg
//         width={size}
//         height={size}
//         viewBox="0 0 24 24"
//         fill="none"
//         className={className}
//       >
//         <path
//           d="M6.76 10.45c0 .28.03.5.08.67.06.16.14.34.25.53a.32.32 0 0 1 .05.17c0 .07-.04.15-.13.22l-.44.3a.32.32 0 0 1-.18.06c-.07 0-.14-.03-.2-.1a2.1 2.1 0 0 1-.25-.32 5.4 5.4 0 0 1-.21-.4c-.54.63-1.21.94-2.02.94-.58 0-1.04-.16-1.38-.5-.34-.32-.51-.75-.51-1.29 0-.57.2-1.03.61-1.37.41-.35.95-.52 1.64-.52.23 0 .46.02.7.05.25.04.5.09.77.15v-.49c0-.51-.1-.87-.32-1.08-.21-.22-.57-.32-1.09-.32-.23 0-.47.03-.71.08-.24.06-.48.13-.71.23a1.9 1.9 0 0 1-.22.08.39.39 0 0 1-.1.02c-.09 0-.13-.06-.13-.19v-.3c0-.1.01-.17.04-.22a.43.43 0 0 1 .17-.13c.23-.12.51-.22.83-.3.33-.09.67-.13 1.04-.13.79 0 1.37.18 1.74.54.37.36.55.9.55 1.63v2.15zm-2.8.96c.21 0 .43-.04.66-.12.23-.08.43-.23.61-.43.1-.12.18-.25.22-.4.04-.14.07-.32.07-.53v-.25a5.46 5.46 0 0 0-1.28-.15c-.46 0-.8.09-1.02.28-.23.19-.34.45-.34.8 0 .32.08.56.25.73.16.18.4.07.83.07zm6.93.93c-.11 0-.18-.02-.23-.06-.05-.04-.1-.13-.14-.25L8.67 5.86a1.1 1.1 0 0 1-.06-.26c0-.1.05-.16.15-.16h.62c.12 0 .2.02.24.06.05.04.09.13.13.25l1.36 5.37 1.27-5.37c.03-.13.07-.21.12-.25.05-.04.14-.06.25-.06h.5c.12 0 .2.02.25.06.05.04.1.13.12.25l1.28 5.44 1.4-5.44c.04-.13.08-.21.13-.25.05-.04.12-.06.24-.06h.59c.1 0 .15.05.15.16 0 .03 0 .07-.01.11l-.05.15-1.91 6.17c-.04.13-.08.21-.13.25-.05.04-.13.06-.23.06h-.54c-.12 0-.2-.02-.25-.06-.05-.05-.09-.13-.12-.26l-1.26-5.2-1.26 5.2c-.03.13-.07.21-.12.26-.05.04-.14.06-.25.06h-.55zm10.2.26c-.33 0-.66-.04-.97-.12a2.8 2.8 0 0 1-.74-.28.44.44 0 0 1-.18-.2.5.5 0 0 1-.03-.2v-.31c0-.13.05-.2.14-.2.04 0 .07.01.11.02.04.02.09.04.14.07.2.09.4.15.62.2.22.04.44.07.66.07.35 0 .62-.06.81-.19.2-.12.3-.31.3-.55 0-.16-.05-.3-.16-.41-.11-.11-.3-.21-.59-.31l-.84-.26c-.42-.14-.73-.33-.93-.6a1.43 1.43 0 0 1-.3-.87c0-.25.05-.48.16-.67.11-.2.25-.37.44-.5.18-.14.39-.25.63-.32.24-.07.49-.1.76-.1.13 0 .27.01.4.03.14.02.27.04.4.07.12.03.24.06.35.1.11.04.2.08.26.12.1.06.17.12.21.19a.5.5 0 0 1 .05.23v.29c0 .13-.05.2-.14.2a.59.59 0 0 1-.22-.07 2.6 2.6 0 0 0-1.1-.23c-.32 0-.57.05-.74.16-.18.11-.27.28-.27.52 0 .16.06.3.17.41.12.11.33.22.64.32l.82.26c.42.13.72.32.9.57.19.25.28.53.28.84 0 .26-.05.49-.15.7-.1.2-.25.38-.44.53-.19.14-.42.25-.68.33-.27.07-.55.11-.85.11z"
//           fill="#FF9900"
//         />
//       </svg>
//     );
//   }
//   // Azure icon
//   return (
//     <svg
//       width={size}
//       height={size}
//       viewBox="0 0 24 24"
//       fill="none"
//       className={className}
//     >
//       <path
//         d="M13.05 4.24L6.56 18.05l4.73.83L22 4.24H13.05zM12.1 5.88L8.03 17.45l-5.53.96L8.24 9.5l3.86-3.62z"
//         fill="#0089D6"
//       />
//     </svg>
//   );
// }

// function ValidationBadge({ status }: { status: Credential["validationStatus"] }) {
//   const map = {
//     VALID: { label: "Valid", class: "bg-emerald-500/15 text-emerald-400 border-emerald-500/30" },
//     INVALID: { label: "Invalid", class: "bg-red-500/15 text-red-400 border-red-500/30" },
//     UNVERIFIED: { label: "Unverified", class: "bg-amber-500/15 text-amber-400 border-amber-500/30" },
//   };
//   const config = map[status];
//   return (
//     <span
//       className={cn(
//         "rounded-full border px-2 py-0.5 font-mono text-[10px] font-semibold tracking-wider",
//         config.class
//       )}
//     >
//       {config.label}
//     </span>
//   );
// }

// function CopyButton({ value }: { value: string }) {
//   const [copied, setCopied] = useState(false);
//   const handleCopy = async () => {
//     await navigator.clipboard.writeText(value);
//     setCopied(true);
//     setTimeout(() => setCopied(false), 2000);
//   };
//   return (
//     <button
//       onClick={handleCopy}
//       className="rounded p-1 text-slate-600 transition-colors hover:bg-slate-700/50 hover:text-slate-300"
//       title="Copy ID"
//     >
//       {copied
//         ? <Check size={12} className="text-emerald-400" />
//         : <Copy size={12} />
//       }
//     </button>
//   );
// }

// function SecretInput({
//   id,
//   label,
//   value,
//   onChange,
//   placeholder,
//   required,
// }: {
//   id: string;
//   label: string;
//   value: string;
//   onChange: (v: string) => void;
//   placeholder?: string;
//   required?: boolean;
// }) {
//   const [show, setShow] = useState(false);
//   return (
//     <div className="space-y-1.5">
//       <Label htmlFor={id} className="font-mono text-xs text-slate-400">
//         {label}
//         {required && <span className="ml-1 text-cyan-400">*</span>}
//       </Label>
//       <div className="relative">
//         <Input
//           id={id}
//           type={show ? "text" : "password"}
//           value={value}
//           onChange={(e) => onChange(e.target.value)}
//           placeholder={placeholder}
//           required={required}
//           className="border-slate-700 bg-slate-900/70 pr-9 font-mono text-xs text-slate-200 placeholder:text-slate-600 focus:border-cyan-500/50 focus:ring-cyan-500/20"
//         />
//         <button
//           type="button"
//           onClick={() => setShow((s) => !s)}
//           className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300"
//         >
//           {show ? <EyeOff size={13} /> : <Eye size={13} />}
//         </button>
//       </div>
//     </div>
//   );
// }

// // ── Credential Card ──────────────────────────────────────────────────────────

// function CredentialCard({
//   credential,
//   onDelete,
// }: {
//   credential: Credential;
//   onDelete: (id: string) => void;
// }) {
//   const [deleting, setDeleting] = useState(false);
//   const { toast } = useToast();

//   async function handleDelete() {
//     setDeleting(true);
//     try {
//       await api.delete(`/credentials/${credential.credentialId}`);
//       toast({ title: "Credential removed", description: credential.credentialLabel });
//       onDelete(credential.credentialId);
//     } catch {
//       toast({
//         title: "Delete failed",
//         description: "Could not remove the credential.",
//         variant: "destructive",
//       });
//     } finally {
//       setDeleting(false);
//     }
//   }

//   const metaLines =
//     credential.provider === "AWS"
//       ? [
//         { key: "Key ID", val: credential.accessKeyIdMasked ?? "—" },
//         { key: "Region", val: credential.defaultRegion ?? "—" },
//       ]
//       : [
//         { key: "Client ID", val: credential.clientIdMasked ?? "—" },
//         { key: "Subscription", val: credential.subscriptionIdMasked ?? "—" },
//       ];

//   return (
//     <div
//       className={cn(
//         "group relative overflow-hidden rounded-xl",
//         "border border-slate-800/80 bg-slate-900/40",
//         "backdrop-blur-sm transition-all duration-300",
//         "hover:border-slate-700/80 hover:bg-slate-900/60",
//         "hover:shadow-lg hover:shadow-slate-950/50"
//       )}
//     >
//       {/* Top colour accent line */}
//       <div
//         className={cn(
//           "absolute inset-x-0 top-0 h-px",
//           credential.provider === "AWS"
//             ? "bg-gradient-to-r from-transparent via-orange-400/40 to-transparent"
//             : "bg-gradient-to-r from-transparent via-sky-400/40 to-transparent"
//         )}
//       />

//       <div className="p-4">
//         {/* Header row */}
//         <div className="mb-3 flex items-start justify-between gap-2">
//           <div className="flex items-center gap-2.5">
//             <div
//               className={cn(
//                 "flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ring-1",
//                 credential.provider === "AWS"
//                   ? "bg-orange-500/10 ring-orange-500/25"
//                   : "bg-sky-500/10 ring-sky-500/25"
//               )}
//             >
//               <ProviderIcon provider={credential.provider} size={18} />
//             </div>
//             <div>
//               <p className="font-semibold text-slate-100 leading-tight">
//                 {credential.credentialLabel}
//               </p>
//               <p className="font-mono text-[10px] text-slate-500 mt-0.5">
//                 {credential.provider}
//               </p>
//             </div>
//           </div>
//           <ValidationBadge status={credential.validationStatus} />
//         </div>

//         {/* Meta rows */}
//         <div className="mb-3 space-y-1.5 rounded-lg bg-slate-950/40 px-3 py-2">
//           {metaLines.map((m) => (
//             <div key={m.key} className="flex items-center justify-between">
//               <span className="font-mono text-[10px] text-slate-600">{m.key}</span>
//               <span className="font-mono text-[10px] text-slate-400">{m.val}</span>
//             </div>
//           ))}
//           <div className="flex items-center justify-between">
//             <span className="font-mono text-[10px] text-slate-600">ID</span>
//             <div className="flex items-center gap-1">
//               <span className="font-mono text-[10px] text-slate-400">
//                 {credential.credentialId}
//               </span>
//               <CopyButton value={credential.credentialId} />
//             </div>
//           </div>
//         </div>

//         {/* Footer */}
//         <div className="flex items-center justify-between">
//           <span className="font-mono text-[10px] text-slate-600">
//             Added{" "}
//             {new Date(credential.createdAt).toLocaleDateString("en-US", {
//               year: "numeric", month: "short", day: "numeric",
//             })}
//           </span>
//           <button
//             onClick={handleDelete}
//             disabled={deleting}
//             className={cn(
//               "flex items-center gap-1.5 rounded-md px-2.5 py-1.5",
//               "font-mono text-[10px] text-slate-600 transition-all duration-200",
//               "hover:bg-red-500/10 hover:text-red-400",
//               "disabled:pointer-events-none disabled:opacity-40"
//             )}
//           >
//             {deleting
//               ? <Loader2 size={11} className="animate-spin" />
//               : <Trash2 size={11} />
//             }
//             Remove
//           </button>
//         </div>
//       </div>
//     </div>
//   );
// }

// // ── Empty State ──────────────────────────────────────────────────────────────

// function EmptyState({ onAdd }: { onAdd: () => void }) {
//   return (
//     <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-slate-800 bg-slate-900/20 px-6 py-20 text-center">
//       <div className="mb-5 flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-800/60 ring-1 ring-slate-700/50">
//         <Lock size={28} className="text-slate-500" />
//       </div>
//       <p className="mb-1 text-base font-semibold text-slate-300">
//         No credentials vaulted
//       </p>
//       <p className="mb-6 max-w-xs text-sm text-slate-500">
//         Securely store your AWS IAM keys or Azure Service Principal credentials.
//         All secrets are encrypted at rest with AES-256/GCM.
//       </p>
//       <button
//         onClick={onAdd}
//         className={cn(
//           "flex items-center gap-2 rounded-lg bg-cyan-500/15 px-4 py-2.5",
//           "font-medium text-sm text-cyan-400 ring-1 ring-cyan-500/30",
//           "transition-all hover:bg-cyan-500/25 hover:ring-cyan-500/50"
//         )}
//       >
//         <Plus size={15} />
//         Add your first credential
//       </button>
//     </div>
//   );
// }

// // ── Add Credential Modal ─────────────────────────────────────────────────────

// function AddCredentialModal({
//   open,
//   onClose,
//   onSuccess,
// }: {
//   open: boolean;
//   onClose: () => void;
//   onSuccess: (cred: Credential) => void;
// }) {
//   const { toast } = useToast();
//   const [provider, setProvider] = useState<Provider | "">("");
//   const [submitting, setSubmitting] = useState(false);
//   const [errors, setErrors] = useState<Record<string, string>>({});

//   // Per-provider form state
//   const [awsForm, setAwsForm] = useState<AwsFormData>(INITIAL_AWS);
//   const [azureForm, setAzureForm] = useState<AzureFormData>(INITIAL_AZURE);

//   // Reset everything when the modal opens/closes
//   useEffect(() => {
//     if (!open) {
//       setProvider("");
//       setAwsForm(INITIAL_AWS);
//       setAzureForm(INITIAL_AZURE);
//       setErrors({});
//       setSubmitting(false);
//     }
//   }, [open]);

//   function patchAws(key: keyof AwsFormData, val: string) {
//     setAwsForm((prev) => ({ ...prev, [key]: val }));
//     setErrors((prev) => { const n = { ...prev }; delete n[key]; return n; });
//   }

//   function patchAzure(key: keyof AzureFormData, val: string) {
//     setAzureForm((prev) => ({ ...prev, [key]: val }));
//     setErrors((prev) => { const n = { ...prev }; delete n[key]; return n; });
//   }

//   function validate(): boolean {
//     const errs: Record<string, string> = {};
//     if (provider === "AWS") {
//       if (!awsForm.credentialLabel.trim()) errs.credentialLabel = "Label is required.";
//       if (!awsForm.accessKeyId.trim()) errs.accessKeyId = "Access Key ID is required.";
//       if (!awsForm.secretAccessKey.trim()) errs.secretAccessKey = "Secret Access Key is required.";
//       if (!awsForm.defaultRegion) errs.defaultRegion = "Region is required.";
//     } else if (provider === "AZURE") {
//       if (!azureForm.credentialLabel.trim()) errs.credentialLabel = "Label is required.";
//       if (!azureForm.tenantId.trim()) errs.tenantId = "Tenant ID is required.";
//       if (!azureForm.clientId.trim()) errs.clientId = "Client ID is required.";
//       if (!azureForm.clientSecret.trim()) errs.clientSecret = "Client Secret is required.";
//       if (!azureForm.subscriptionId.trim()) errs.subscriptionId = "Subscription ID is required.";
//     }
//     setErrors(errs);
//     return Object.keys(errs).length === 0;
//   }

//   async function handleSubmit(e: React.FormEvent) {
//     e.preventDefault();
//     if (!provider || !validate()) return;

//     setSubmitting(true);
//     try {
//       const endpoint = provider === "AWS" ? "/credentials/aws" : "/credentials/azure";
//       const payload = provider === "AWS" ? awsForm : azureForm;

//       const response = await api.post(endpoint, payload);
//       const saved: Credential = response.data;

//       toast({
//         title: "Credential saved",
//         description: `${saved.credentialLabel} has been vaulted successfully.`,
//       });
//       onSuccess(saved);
//       onClose();
//     } catch (err: any) {
//       const msg =
//         err?.response?.data?.message ??
//         err?.response?.data?.error ??
//         "Failed to save credential. Please check your inputs.";
//       toast({ title: "Save failed", description: msg, variant: "destructive" });
//     } finally {
//       setSubmitting(false);
//     }
//   }

//   // ── Field component shortcuts ──────────────────────────────────────────────

//   function TextField({
//     id,
//     label,
//     value,
//     onChange,
//     placeholder,
//     hint,
//   }: {
//     id: string;
//     label: string;
//     value: string;
//     onChange: (v: string) => void;
//     placeholder?: string;
//     hint?: string;
//   }) {
//     return (
//       <div className="space-y-1.5">
//         <Label htmlFor={id} className="font-mono text-xs text-slate-400">
//           {label} <span className="text-cyan-400">*</span>
//         </Label>
//         <Input
//           id={id}
//           value={value}
//           onChange={(e) => onChange(e.target.value)}
//           placeholder={placeholder}
//           className={cn(
//             "border-slate-700 bg-slate-900/70 font-mono text-xs text-slate-200",
//             "placeholder:text-slate-600 focus:border-cyan-500/50 focus:ring-cyan-500/20",
//             errors[id] && "border-red-500/60"
//           )}
//         />
//         {hint && !errors[id] && (
//           <p className="font-mono text-[10px] text-slate-600">{hint}</p>
//         )}
//         {errors[id] && (
//           <p className="flex items-center gap-1 font-mono text-[10px] text-red-400">
//             <AlertCircle size={10} /> {errors[id]}
//           </p>
//         )}
//       </div>
//     );
//   }

//   return (
//     <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
//       <DialogContent
//         className={cn(
//           "max-h-[90vh] overflow-y-auto",
//           "border-slate-800 bg-slate-950 text-slate-200",
//           "shadow-2xl shadow-black/60",
//           "sm:max-w-lg"
//         )}
//       >
//         {/* Header */}
//         <DialogHeader className="border-b border-slate-800/80 pb-4">
//           <div className="flex items-center gap-2.5">
//             <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-cyan-500/15 ring-1 ring-cyan-500/30">
//               <Key size={14} className="text-cyan-400" />
//             </div>
//             <div>
//               <DialogTitle className="font-mono text-sm tracking-wide text-slate-100">
//                 ADD CREDENTIAL
//               </DialogTitle>
//               <DialogDescription className="font-mono text-[10px] text-slate-500">
//                 Encrypted with AES-256/GCM at rest
//               </DialogDescription>
//             </div>
//           </div>
//         </DialogHeader>

//         <form onSubmit={handleSubmit} className="space-y-5 pt-2">

//           {/* ── Step 1: Provider selection ──────────────────────── */}
//           <div>
//             <p className="mb-2 font-mono text-[10px] font-semibold tracking-[0.15em] text-slate-500">
//               STEP 1 — SELECT PROVIDER
//             </p>
//             <div className="grid grid-cols-2 gap-2">
//               {(["AWS", "AZURE"] as Provider[]).map((p) => (
//                 <button
//                   key={p}
//                   type="button"
//                   onClick={() => { setProvider(p); setErrors({}); }}
//                   className={cn(
//                     "flex flex-col items-center gap-2.5 rounded-xl border px-4 py-4",
//                     "transition-all duration-200",
//                     provider === p
//                       ? p === "AWS"
//                         ? "border-orange-500/50 bg-orange-500/10 ring-1 ring-orange-500/20"
//                         : "border-sky-500/50 bg-sky-500/10 ring-1 ring-sky-500/20"
//                       : "border-slate-800 bg-slate-900/40 hover:border-slate-700 hover:bg-slate-900/70"
//                   )}
//                 >
//                   <ProviderIcon provider={p} size={26} />
//                   <span className="font-mono text-xs font-semibold text-slate-300">
//                     {p === "AWS" ? "Amazon Web Services" : "Microsoft Azure"}
//                   </span>
//                 </button>
//               ))}
//             </div>
//           </div>

//           {/* ── Step 2: Provider-specific fields ────────────────── */}
//           {provider && (
//             <div className="space-y-3">
//               <p className="font-mono text-[10px] font-semibold tracking-[0.15em] text-slate-500">
//                 STEP 2 — ENTER CREDENTIALS
//               </p>

//               {/* Shared: Label */}
//               <TextField
//                 id="credentialLabel"
//                 label="Credential Label"
//                 value={
//                   provider === "AWS"
//                     ? awsForm.credentialLabel
//                     : azureForm.credentialLabel
//                 }
//                 onChange={(v) =>
//                   provider === "AWS"
//                     ? patchAws("credentialLabel", v)
//                     : patchAzure("credentialLabel", v)
//                 }
//                 placeholder="e.g. Production AWS Account"
//               />

//               {/* ── AWS-specific ── */}
//               {provider === "AWS" && (
//                 <>
//                   <TextField
//                     id="accessKeyId"
//                     label="Access Key ID"
//                     value={awsForm.accessKeyId}
//                     onChange={(v) => patchAws("accessKeyId", v)}
//                     placeholder="AKIAIOSFODNN7EXAMPLE"
//                     hint="Starts with AKIA, ASIA, AROA, etc."
//                   />
//                   <SecretInput
//                     id="secretAccessKey"
//                     label="Secret Access Key"
//                     value={awsForm.secretAccessKey}
//                     onChange={(v) => patchAws("secretAccessKey", v)}
//                     placeholder="wJalrXUtnFEMI/K7MDENG/bPxRfi..."
//                     required
//                   />
//                   {errors.secretAccessKey && (
//                     <p className="flex items-center gap-1 font-mono text-[10px] text-red-400">
//                       <AlertCircle size={10} /> {errors.secretAccessKey}
//                     </p>
//                   )}

//                   {/* Region select */}
//                   <div className="space-y-1.5">
//                     <Label className="font-mono text-xs text-slate-400">
//                       Default Region <span className="text-cyan-400">*</span>
//                     </Label>
//                     <Select
//                       value={awsForm.defaultRegion}
//                       onValueChange={(v) => patchAws("defaultRegion", v)}
//                     >
//                       <SelectTrigger className="border-slate-700 bg-slate-900/70 font-mono text-xs text-slate-200 focus:ring-cyan-500/20">
//                         <SelectValue placeholder="Select region" />
//                       </SelectTrigger>
//                       <SelectContent className="border-slate-700 bg-slate-900">
//                         {AWS_REGIONS.map((r) => (
//                           <SelectItem
//                             key={r.value}
//                             value={r.value}
//                             className="font-mono text-xs text-slate-300 focus:bg-slate-800 focus:text-slate-100"
//                           >
//                             {r.label}
//                           </SelectItem>
//                         ))}
//                       </SelectContent>
//                     </Select>
//                     {errors.defaultRegion && (
//                       <p className="flex items-center gap-1 font-mono text-[10px] text-red-400">
//                         <AlertCircle size={10} /> {errors.defaultRegion}
//                       </p>
//                     )}
//                   </div>
//                 </>
//               )}

//               {/* ── Azure-specific ── */}
//               {provider === "AZURE" && (
//                 <>
//                   <TextField
//                     id="tenantId"
//                     label="Tenant ID"
//                     value={azureForm.tenantId}
//                     onChange={(v) => patchAzure("tenantId", v)}
//                     placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
//                     hint="Your Azure AD Tenant (Directory) ID"
//                   />
//                   <TextField
//                     id="clientId"
//                     label="Client ID (App Registration)"
//                     value={azureForm.clientId}
//                     onChange={(v) => patchAzure("clientId", v)}
//                     placeholder="yyyyyyyy-yyyy-yyyy-yyyy-yyyyyyyyyyyy"
//                   />
//                   <SecretInput
//                     id="clientSecret"
//                     label="Client Secret"
//                     value={azureForm.clientSecret}
//                     onChange={(v) => patchAzure("clientSecret", v)}
//                     placeholder="your~client~secret~value"
//                     required
//                   />
//                   {errors.clientSecret && (
//                     <p className="flex items-center gap-1 font-mono text-[10px] text-red-400">
//                       <AlertCircle size={10} /> {errors.clientSecret}
//                     </p>
//                   )}
//                   <TextField
//                     id="subscriptionId"
//                     label="Subscription ID"
//                     value={azureForm.subscriptionId}
//                     onChange={(v) => patchAzure("subscriptionId", v)}
//                     placeholder="zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz"
//                   />
//                 </>
//               )}
//             </div>
//           )}

//           {/* Encryption notice */}
//           {provider && (
//             <div className="flex items-start gap-2.5 rounded-lg border border-emerald-500/20 bg-emerald-500/5 px-3 py-2.5">
//               <ShieldCheck size={13} className="mt-0.5 shrink-0 text-emerald-400" />
//               <p className="font-mono text-[10px] text-emerald-400/80">
//                 All secrets are encrypted client-to-server over TLS and stored with
//                 AES-256/GCM encryption. A unique IV is generated per field.
//                 Raw values are never returned by the API.
//               </p>
//             </div>
//           )}

//           {/* Actions */}
//           <div className="flex items-center justify-end gap-2.5 border-t border-slate-800/80 pt-4">
//             <Button
//               type="button"
//               variant="ghost"
//               onClick={onClose}
//               disabled={submitting}
//               className="font-mono text-xs text-slate-400 hover:text-slate-200"
//             >
//               Cancel
//             </Button>
//             <Button
//               type="submit"
//               disabled={!provider || submitting}
//               className={cn(
//                 "font-mono text-xs",
//                 "bg-cyan-500/15 text-cyan-400 ring-1 ring-cyan-500/30",
//                 "hover:bg-cyan-500/25 hover:ring-cyan-500/50",
//                 "disabled:pointer-events-none disabled:opacity-40"
//               )}
//             >
//               {submitting ? (
//                 <>
//                   <Loader2 size={12} className="mr-2 animate-spin" />
//                   Encrypting & Saving...
//                 </>
//               ) : (
//                 <>
//                   <Lock size={12} className="mr-2" />
//                   Save to Vault
//                 </>
//               )}
//             </Button>
//           </div>
//         </form>
//       </DialogContent>
//     </Dialog>
//   );
// }

// // ── Page Header ──────────────────────────────────────────────────────────────

// function PageHeader({
//   count,
//   loading,
//   onRefresh,
//   onAdd,
// }: {
//   count: number;
//   loading: boolean;
//   onRefresh: () => void;
//   onAdd: () => void;
// }) {
//   return (
//     <div className="mb-8 flex items-start justify-between gap-4">
//       <div>
//         <div className="mb-1 flex items-center gap-2.5">
//           <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-500/15 ring-1 ring-cyan-500/30">
//             <Key size={16} className="text-cyan-400" />
//           </div>
//           <div>
//             <h1 className="font-mono text-xl font-bold tracking-tight text-slate-100">
//               CREDENTIAL VAULT
//             </h1>
//             <p className="font-mono text-[10px] tracking-widest text-slate-500">
//               {count} STORED SECRET{count !== 1 ? "S" : ""} · AES-256/GCM ENCRYPTED
//             </p>
//           </div>
//         </div>
//         <p className="mt-2 max-w-lg text-sm text-slate-500">
//           Securely store and manage cloud provider credentials. Keys are encrypted at
//           rest and decrypted only at execution time.
//         </p>
//       </div>

//       <div className="flex shrink-0 items-center gap-2">
//         <button
//           onClick={onRefresh}
//           disabled={loading}
//           className={cn(
//             "flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-800/60",
//             "px-3 py-2 font-mono text-xs text-slate-400 transition-all",
//             "hover:border-slate-600 hover:text-slate-200",
//             "disabled:pointer-events-none disabled:opacity-40"
//           )}
//         >
//           <RefreshCw size={13} className={loading ? "animate-spin" : ""} />
//           Refresh
//         </button>
//         <button
//           onClick={onAdd}
//           className={cn(
//             "flex items-center gap-2 rounded-lg bg-cyan-500/15 px-4 py-2",
//             "font-mono text-xs font-semibold text-cyan-400 ring-1 ring-cyan-500/30",
//             "transition-all hover:bg-cyan-500/25 hover:ring-cyan-500/50"
//           )}
//         >
//           <Plus size={13} />
//           Add Credential
//         </button>
//       </div>
//     </div>
//   );
// }

// // ── Main Page ────────────────────────────────────────────────────────────────

// export default function CredentialsPage() {
//   const [credentials, setCredentials] = useState<Credential[]>([]);
//   const [loading, setLoading] = useState(true);
//   const [error, setError] = useState<string | null>(null);
//   const [modalOpen, setModalOpen] = useState(false);

//   const { toast } = useToast();

//   const fetchCredentials = useCallback(async () => {
//     setLoading(true);
//     setError(null);
//     try {
//       const res = await api.get("/credentials");
//       // API returns { credentials: [...] } or directly an array
//       const list: Credential[] = Array.isArray(res.data)
//         ? res.data
//         : res.data.credentials ?? [];
//       setCredentials(list);
//     } catch (err: any) {
//       const msg =
//         err?.response?.data?.message ?? "Failed to load credentials.";
//       setError(msg);
//       toast({ title: "Error", description: msg, variant: "destructive" });
//     } finally {
//       setLoading(false);
//     }
//   }, []);

//   useEffect(() => {
//     fetchCredentials();
//   }, [fetchCredentials]);

//   function handleCredentialAdded(newCred: Credential) {
//     setCredentials((prev) => [newCred, ...prev]);
//   }

//   function handleCredentialDeleted(id: string) {
//     setCredentials((prev) => prev.filter((c) => c.credentialId !== id));
//   }

//   return (
//     <>
//       <div className="min-h-[calc(100vh-4rem)]">
//         <PageHeader
//           count={credentials.length}
//           loading={loading}
//           onRefresh={fetchCredentials}
//           onAdd={() => setModalOpen(true)}
//         />

//         {/* Loading skeleton */}
//         {loading && (
//           <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
//             {[...Array(3)].map((_, i) => (
//               <div
//                 key={i}
//                 className="animate-pulse rounded-xl border border-slate-800 bg-slate-900/40 p-4"
//                 style={{ animationDelay: `${i * 100}ms` }}
//               >
//                 <div className="mb-3 flex items-center gap-2.5">
//                   <div className="h-9 w-9 rounded-lg bg-slate-800" />
//                   <div className="space-y-1.5">
//                     <div className="h-3.5 w-32 rounded bg-slate-800" />
//                     <div className="h-2.5 w-16 rounded bg-slate-800/60" />
//                   </div>
//                 </div>
//                 <div className="space-y-2 rounded-lg bg-slate-950/40 p-3">
//                   <div className="h-2.5 w-full rounded bg-slate-800/60" />
//                   <div className="h-2.5 w-4/5 rounded bg-slate-800/60" />
//                   <div className="h-2.5 w-3/4 rounded bg-slate-800/60" />
//                 </div>
//               </div>
//             ))}
//           </div>
//         )}

//         {/* Error state */}
//         {!loading && error && (
//           <div className="flex flex-col items-center rounded-xl border border-red-500/20 bg-red-500/5 px-6 py-12 text-center">
//             <AlertCircle size={32} className="mb-3 text-red-400" />
//             <p className="font-mono text-sm text-red-400">{error}</p>
//             <button
//               onClick={fetchCredentials}
//               className="mt-4 font-mono text-xs text-slate-500 underline underline-offset-4 hover:text-slate-300"
//             >
//               Try again
//             </button>
//           </div>
//         )}

//         {/* Empty state */}
//         {!loading && !error && credentials.length === 0 && (
//           <EmptyState onAdd={() => setModalOpen(true)} />
//         )}

//         {/* Credentials grid */}
//         {!loading && !error && credentials.length > 0 && (
//           <>
//             {/* Provider summary strip */}
//             <div className="mb-5 flex items-center gap-4">
//               {(["AWS", "AZURE"] as Provider[]).map((p) => {
//                 const count = credentials.filter((c) => c.provider === p).length;
//                 if (count === 0) return null;
//                 return (
//                   <div
//                     key={p}
//                     className="flex items-center gap-2 rounded-lg border border-slate-800 bg-slate-900/40 px-3 py-1.5"
//                   >
//                     <ProviderIcon provider={p} size={13} />
//                     <span className="font-mono text-xs text-slate-400">
//                       {count} {p}
//                     </span>
//                   </div>
//                 );
//               })}
//             </div>

//             <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
//               {credentials.map((cred) => (
//                 <CredentialCard
//                   key={cred.credentialId}
//                   credential={cred}
//                   onDelete={handleCredentialDeleted}
//                 />
//               ))}
//             </div>
//           </>
//         )}
//       </div>

//       <AddCredentialModal
//         open={modalOpen}
//         onClose={() => setModalOpen(false)}
//         onSuccess={handleCredentialAdded}
//       />

//       <Toaster />
//     </>
//   );
// }

// "use client";

// import { useState, useEffect, useCallback, memo } from "react";
// import {
//   Key,
//   Plus,
//   Cloud,
//   Server,
//   Trash2,
//   RefreshCw,
//   ShieldCheck,
//   AlertCircle,
//   Copy,
//   Check,
//   ChevronDown,
//   Loader2,
//   Lock,
//   Eye,
//   EyeOff,
// } from "lucide-react";
// import { cn } from "@/lib/utils";
// import api from "@/lib/api";

// // ── Shadcn UI imports ────────────────────────────────────────────────────────
// import {
//   Dialog,
//   DialogContent,
//   DialogHeader,
//   DialogTitle,
//   DialogDescription,
// } from "@/components/ui/dialog";
// import { Button } from "@/components/ui/button";
// import { Input } from "@/components/ui/input";
// import { Label } from "@/components/ui/label";
// import { Badge } from "@/components/ui/badge";
// import {
//   Select,
//   SelectContent,
//   SelectItem,
//   SelectTrigger,
//   SelectValue,
// } from "@/components/ui/select";
// import { useToast } from "@/components/ui/use-toast";
// import { Toaster } from "@/components/ui/toaster";

// // ── Types ────────────────────────────────────────────────────────────────────

// type Provider = "AWS" | "AZURE";

// interface Credential {
//   credentialId: string;
//   provider: Provider;
//   credentialLabel: string;
//   validationStatus: "VALID" | "INVALID" | "UNVERIFIED";
//   createdAt: string;
//   accessKeyIdMasked?: string;
//   defaultRegion?: string;
//   clientIdMasked?: string;
//   subscriptionIdMasked?: string;
// }

// interface AwsFormData {
//   credentialLabel: string;
//   accessKeyId: string;
//   secretAccessKey: string;
//   defaultRegion: string;
// }

// interface AzureFormData {
//   credentialLabel: string;
//   tenantId: string;
//   clientId: string;
//   clientSecret: string;
//   subscriptionId: string;
// }

// // ── Constants ────────────────────────────────────────────────────────────────

// const AWS_REGIONS = [
//   { value: "us-east-1", label: "US East (N. Virginia)" },
//   { value: "us-east-2", label: "US East (Ohio)" },
//   { value: "us-west-1", label: "US West (N. California)" },
//   { value: "us-west-2", label: "US West (Oregon)" },
//   { value: "ap-south-1", label: "Asia Pacific (Mumbai)" },
//   { value: "ap-southeast-1", label: "Asia Pacific (Singapore)" },
//   { value: "ap-southeast-2", label: "Asia Pacific (Sydney)" },
//   { value: "ap-northeast-1", label: "Asia Pacific (Tokyo)" },
//   { value: "eu-west-1", label: "Europe (Ireland)" },
//   { value: "eu-west-2", label: "Europe (London)" },
//   { value: "eu-central-1", label: "Europe (Frankfurt)" },
// ];

// const INITIAL_AWS: AwsFormData = {
//   credentialLabel: "",
//   accessKeyId: "",
//   secretAccessKey: "",
//   defaultRegion: "us-east-1",
// };

// const INITIAL_AZURE: AzureFormData = {
//   credentialLabel: "",
//   tenantId: "",
//   clientId: "",
//   clientSecret: "",
//   subscriptionId: "",
// };

// // ── Helper sub-components ────────────────────────────────────────────────────

// function ProviderIcon({ provider, size = 18, className }: { provider: Provider; size?: number; className?: string; }) {
//   if (provider === "AWS") {
//     return (
//       <svg width={size} height={size} viewBox="0 0 24 24" fill="none" className={className}>
//         <path d="M6.76 10.45c0 .28.03.5.08.67.06.16.14.34.25.53a.32.32 0 0 1 .05.17c0 .07-.04.15-.13.22l-.44.3a.32.32 0 0 1-.18.06c-.07 0-.14-.03-.2-.1a2.1 2.1 0 0 1-.25-.32 5.4 5.4 0 0 1-.21-.4c-.54.63-1.21.94-2.02.94-.58 0-1.04-.16-1.38-.5-.34-.32-.51-.75-.51-1.29 0-.57.2-1.03.61-1.37.41-.35.95-.52 1.64-.52.23 0 .46.02.7.05.25.04.5.09.77.15v-.49c0-.51-.1-.87-.32-1.08-.21-.22-.57-.32-1.09-.32-.23 0-.47.03-.71.08-.24.06-.48.13-.71.23a1.9 1.9 0 0 1-.22.08.39.39 0 0 1-.1.02c-.09 0-.13-.06-.13-.19v-.3c0-.1.01-.17.04-.22a.43.43 0 0 1 .17-.13c.23-.12.51-.22.83-.3.33-.09.67-.13 1.04-.13.79 0 1.37.18 1.74.54.37.36.55.9.55 1.63v2.15zm-2.8.96c.21 0 .43-.04.66-.12.23-.08.43-.23.61-.43.1-.12.18-.25.22-.4.04-.14.07-.32.07-.53v-.25a5.46 5.46 0 0 0-1.28-.15c-.46 0-.8.09-1.02.28-.23.19-.34.45-.34.8 0 .32.08.56.25.73.16.18.4.07.83.07zm6.93.93c-.11 0-.18-.02-.23-.06-.05-.04-.1-.13-.14-.25L8.67 5.86a1.1 1.1 0 0 1-.06-.26c0-.1.05-.16.15-.16h.62c.12 0 .2.02.24.06.05.04.09.13.13.25l1.36 5.37 1.27-5.37c.03-.13.07-.21.12-.25.05-.04.14-.06.25-.06h.5c.12 0 .2.02.25.06.05.04.1.13.12.25l1.28 5.44 1.4-5.44c.04-.13.08-.21.13-.25.05-.04.12-.06.24-.06h.59c.1 0 .15.05.15.16 0 .03 0 .07-.01.11l-.05.15-1.91 6.17c-.04.13-.08.21-.13.25-.05.04-.13.06-.23.06h-.54c-.12 0-.2-.02-.25-.06-.05-.05-.09-.13-.12-.26l-1.26-5.2-1.26 5.2c-.03.13-.07.21-.12.26-.05.04-.14.06-.25.06h-.55zm10.2.26c-.33 0-.66-.04-.97-.12a2.8 2.8 0 0 1-.74-.28.44.44 0 0 1-.18-.2.5.5 0 0 1-.03-.2v-.31c0-.13.05-.2.14-.2.04 0 .07.01.11.02.04.02.09.04.14.07.2.09.4.15.62.2.22.04.44.07.66.07.35 0 .62-.06.81-.19.2-.12.3-.31.3-.55 0-.16-.05-.3-.16-.41-.11-.11-.3-.21-.59-.31l-.84-.26c-.42-.14-.73-.33-.93-.6a1.43 1.43 0 0 1-.3-.87c0-.25.05-.48.16-.67.11-.2.25-.37.44-.5.18-.14.39-.25.63-.32.24-.07.49-.1.76-.1.13 0 .27.01.4.03.14.02.27.04.4.07.12.03.24.06.35.1.11.04.2.08.26.12.1.06.17.12.21.19a.5.5 0 0 1 .05.23v.29c0 .13-.05.2-.14.2a.59.59 0 0 1-.22-.07 2.6 2.6 0 0 0-1.1-.23c-.32 0-.57.05-.74.16-.18.11-.27.28-.27.52 0 .16.06.3.17.41.12.11.33.22.64.32l.82.26c.42.13.72.32.9.57.19.25.28.53.28.84 0 .26-.05.49-.15.7-.1.2-.25.38-.44.53-.19.14-.42.25-.68.33-.27.07-.55.11-.85.11z" fill="#FF9900" />
//       </svg>
//     );
//   }
//   return (
//     <svg width={size} height={size} viewBox="0 0 24 24" fill="none" className={className}>
//       <path d="M13.05 4.24L6.56 18.05l4.73.83L22 4.24H13.05zM12.1 5.88L8.03 17.45l-5.53.96L8.24 9.5l3.86-3.62z" fill="#0089D6" />
//     </svg>
//   );
// }

// function ValidationBadge({ status }: { status: Credential["validationStatus"] }) {
//   const map = {
//     VALID: { label: "Valid", class: "bg-emerald-500/15 text-emerald-400 border-emerald-500/30" },
//     INVALID: { label: "Invalid", class: "bg-red-500/15 text-red-400 border-red-500/30" },
//     UNVERIFIED: { label: "Unverified", class: "bg-amber-500/15 text-amber-400 border-amber-500/30" },
//   };
//   const config = map[status] || map.UNVERIFIED;
//   return (
//     <span className={cn("rounded-full border px-2 py-0.5 font-mono text-[10px] font-semibold tracking-wider", config.class)}>
//       {config.label}
//     </span>
//   );
// }

// function CopyButton({ value }: { value: string }) {
//   const [copied, setCopied] = useState(false);
//   const handleCopy = async () => {
//     await navigator.clipboard.writeText(value);
//     setCopied(true);
//     setTimeout(() => setCopied(false), 2000);
//   };
//   return (
//     <button onClick={handleCopy} className="rounded p-1 text-slate-600 transition-colors hover:bg-slate-700/50 hover:text-slate-300" title="Copy ID">
//       {copied ? <Check size={12} className="text-emerald-400" /> : <Copy size={12} />}
//     </button>
//   );
// }

// // ── FIXED: Extracted TextField and SecretInput Outside the Modal to Prevent Re-mounts ──

// const TextField = memo(function TextField({
//   id,
//   label,
//   value,
//   onChange,
//   placeholder,
//   hint,
//   error
// }: {
//   id: string;
//   label: string;
//   value: string;
//   onChange: (v: string) => void;
//   placeholder?: string;
//   hint?: string;
//   error?: string;
// }) {
//   return (
//     <div className="space-y-1.5">
//       <Label htmlFor={id} className="font-mono text-xs text-slate-400">
//         {label} <span className="text-cyan-400">*</span>
//       </Label>
//       <Input
//         id={id}
//         value={value}
//         onChange={(e) => onChange(e.target.value)}
//         placeholder={placeholder}
//         className={cn(
//           "border-slate-700 bg-slate-900/70 font-mono text-xs text-slate-200",
//           "placeholder:text-slate-600 focus:border-cyan-500/50 focus:ring-cyan-500/20",
//           error && "border-red-500/60"
//         )}
//       />
//       {hint && !error && (
//         <p className="font-mono text-[10px] text-slate-600">{hint}</p>
//       )}
//       {error && (
//         <p className="flex items-center gap-1 font-mono text-[10px] text-red-400">
//           <AlertCircle size={10} /> {error}
//         </p>
//       )}
//     </div>
//   );
// });

// const SecretInput = memo(function SecretInput({
//   id,
//   label,
//   value,
//   onChange,
//   placeholder,
//   required,
//   error
// }: {
//   id: string;
//   label: string;
//   value: string;
//   onChange: (v: string) => void;
//   placeholder?: string;
//   required?: boolean;
//   error?: string;
// }) {
//   const [show, setShow] = useState(false);
//   return (
//     <div className="space-y-1.5">
//       <Label htmlFor={id} className="font-mono text-xs text-slate-400">
//         {label}
//         {required && <span className="ml-1 text-cyan-400">*</span>}
//       </Label>
//       <div className="relative">
//         <Input
//           id={id}
//           type={show ? "text" : "password"}
//           value={value}
//           onChange={(e) => onChange(e.target.value)}
//           placeholder={placeholder}
//           required={required}
//           className={cn(
//             "border-slate-700 bg-slate-900/70 pr-9 font-mono text-xs text-slate-200 placeholder:text-slate-600 focus:border-cyan-500/50 focus:ring-cyan-500/20",
//             error && "border-red-500/60"
//           )}
//         />
//         <button
//           type="button"
//           onClick={() => setShow((s) => !s)}
//           className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300"
//         >
//           {show ? <EyeOff size={13} /> : <Eye size={13} />}
//         </button>
//       </div>
//       {error && (
//         <p className="flex items-center gap-1 font-mono text-[10px] text-red-400 mt-1">
//           <AlertCircle size={10} /> {error}
//         </p>
//       )}
//     </div>
//   );
// });


// // ── Credential Card ──────────────────────────────────────────────────────────

// function CredentialCard({ credential, onDelete }: { credential: Credential; onDelete: (id: string) => void; }) {
//   const [deleting, setDeleting] = useState(false);
//   const { toast } = useToast();

//   async function handleDelete() {
//     // FIXED: Ensure credentialId exists before making API call
//     if (!credential.credentialId) {
//         toast({ title: "Delete failed", description: "Invalid Credential ID.", variant: "destructive" });
//         return;
//     }
//     setDeleting(true);
//     try {
//       await api.delete(`/credentials/${credential.credentialId}`);
//       toast({ title: "Credential removed", description: credential.credentialLabel });
//       onDelete(credential.credentialId);
//     } catch {
//       toast({ title: "Delete failed", description: "Could not remove the credential.", variant: "destructive" });
//     } finally {
//       setDeleting(false);
//     }
//   }

//   const metaLines = credential.provider === "AWS"
//       ? [
//         { key: "Key ID", val: credential.accessKeyIdMasked ?? "—" },
//         { key: "Region", val: credential.defaultRegion ?? "—" },
//       ]
//       : [
//         { key: "Client ID", val: credential.clientIdMasked ?? "—" },
//         { key: "Subscription", val: credential.subscriptionIdMasked ?? "—" },
//       ];

//   return (
//     <div className={cn("group relative overflow-hidden rounded-xl border border-slate-800/80 bg-slate-900/40 backdrop-blur-sm transition-all duration-300 hover:border-slate-700/80 hover:bg-slate-900/60 hover:shadow-lg hover:shadow-slate-950/50")}>
//       <div className={cn("absolute inset-x-0 top-0 h-px", credential.provider === "AWS" ? "bg-gradient-to-r from-transparent via-orange-400/40 to-transparent" : "bg-gradient-to-r from-transparent via-sky-400/40 to-transparent")} />
//       <div className="p-4">
//         <div className="mb-3 flex items-start justify-between gap-2">
//           <div className="flex items-center gap-2.5">
//             <div className={cn("flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ring-1", credential.provider === "AWS" ? "bg-orange-500/10 ring-orange-500/25" : "bg-sky-500/10 ring-sky-500/25")}>
//               <ProviderIcon provider={credential.provider} size={18} />
//             </div>
//             <div>
//               <p className="font-semibold text-slate-100 leading-tight">{credential.credentialLabel}</p>
//               <p className="font-mono text-[10px] text-slate-500 mt-0.5">{credential.provider}</p>
//             </div>
//           </div>
//           <ValidationBadge status={credential.validationStatus} />
//         </div>
//         <div className="mb-3 space-y-1.5 rounded-lg bg-slate-950/40 px-3 py-2">
//           {metaLines.map((m) => (
//             <div key={m.key} className="flex items-center justify-between">
//               <span className="font-mono text-[10px] text-slate-600">{m.key}</span>
//               <span className="font-mono text-[10px] text-slate-400">{m.val}</span>
//             </div>
//           ))}
//           <div className="flex items-center justify-between">
//             <span className="font-mono text-[10px] text-slate-600">ID</span>
//             <div className="flex items-center gap-1">
//               <span className="font-mono text-[10px] text-slate-400">{credential.credentialId || "Unknown"}</span>
//               <CopyButton value={credential.credentialId || ""} />
//             </div>
//           </div>
//         </div>
//         <div className="flex items-center justify-between">
//           <span className="font-mono text-[10px] text-slate-600">Added {credential.createdAt ? new Date(credential.createdAt).toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric" }) : "—"}</span>
//           <button onClick={handleDelete} disabled={deleting} className={cn("flex items-center gap-1.5 rounded-md px-2.5 py-1.5 font-mono text-[10px] text-slate-600 transition-all duration-200 hover:bg-red-500/10 hover:text-red-400 disabled:pointer-events-none disabled:opacity-40")}>
//             {deleting ? <Loader2 size={11} className="animate-spin" /> : <Trash2 size={11} />}
//             Remove
//           </button>
//         </div>
//       </div>
//     </div>
//   );
// }

// // ── Empty State ──────────────────────────────────────────────────────────────

// function EmptyState({ onAdd }: { onAdd: () => void }) {
//   return (
//     <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-slate-800 bg-slate-900/20 px-6 py-20 text-center">
//       <div className="mb-5 flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-800/60 ring-1 ring-slate-700/50">
//         <Lock size={28} className="text-slate-500" />
//       </div>
//       <p className="mb-1 text-base font-semibold text-slate-300">No credentials vaulted</p>
//       <p className="mb-6 max-w-xs text-sm text-slate-500">Securely store your AWS IAM keys or Azure Service Principal credentials. All secrets are encrypted at rest with AES-256/GCM.</p>
//       <button onClick={onAdd} className={cn("flex items-center gap-2 rounded-lg bg-cyan-500/15 px-4 py-2.5 font-medium text-sm text-cyan-400 ring-1 ring-cyan-500/30 transition-all hover:bg-cyan-500/25 hover:ring-cyan-500/50")}>
//         <Plus size={15} /> Add your first credential
//       </button>
//     </div>
//   );
// }

// // ── Add Credential Modal ─────────────────────────────────────────────────────

// function AddCredentialModal({ open, onClose, onSuccess }: { open: boolean; onClose: () => void; onSuccess: (cred: Credential) => void; }) {
//   const { toast } = useToast();
//   const [provider, setProvider] = useState<Provider | "">("");
//   const [submitting, setSubmitting] = useState(false);
//   const [errors, setErrors] = useState<Record<string, string>>({});

//   const [awsForm, setAwsForm] = useState<AwsFormData>(INITIAL_AWS);
//   const [azureForm, setAzureForm] = useState<AzureFormData>(INITIAL_AZURE);

//   useEffect(() => {
//     if (!open) {
//       setProvider("");
//       setAwsForm(INITIAL_AWS);
//       setAzureForm(INITIAL_AZURE);
//       setErrors({});
//       setSubmitting(false);
//     }
//   }, [open]);

//   const patchAws = useCallback((key: keyof AwsFormData, val: string) => {
//     setAwsForm((prev) => ({ ...prev, [key]: val }));
//     setErrors((prev) => { const n = { ...prev }; delete n[key]; return n; });
//   }, []);

//   const patchAzure = useCallback((key: keyof AzureFormData, val: string) => {
//     setAzureForm((prev) => ({ ...prev, [key]: val }));
//     setErrors((prev) => { const n = { ...prev }; delete n[key]; return n; });
//   }, []);

//   function validate(): boolean {
//     const errs: Record<string, string> = {};
//     if (provider === "AWS") {
//       if (!awsForm.credentialLabel.trim()) errs.credentialLabel = "Label is required.";
//       if (!awsForm.accessKeyId.trim()) errs.accessKeyId = "Access Key ID is required.";
//       if (!awsForm.secretAccessKey.trim()) errs.secretAccessKey = "Secret Access Key is required.";
//       if (!awsForm.defaultRegion) errs.defaultRegion = "Region is required.";
//     } else if (provider === "AZURE") {
//       if (!azureForm.credentialLabel.trim()) errs.credentialLabel = "Label is required.";
//       if (!azureForm.tenantId.trim()) errs.tenantId = "Tenant ID is required.";
//       if (!azureForm.clientId.trim()) errs.clientId = "Client ID is required.";
//       if (!azureForm.clientSecret.trim()) errs.clientSecret = "Client Secret is required.";
//       if (!azureForm.subscriptionId.trim()) errs.subscriptionId = "Subscription ID is required.";
//     }
//     setErrors(errs);
//     return Object.keys(errs).length === 0;
//   }

//   async function handleSubmit(e: React.FormEvent) {
//     e.preventDefault();
//     if (!provider || !validate()) return;

//     setSubmitting(true);
//     try {
//       const endpoint = provider === "AWS" ? "/credentials/aws" : "/credentials/azure";
//       const payload = provider === "AWS" ? awsForm : azureForm;

//       const response = await api.post(endpoint, payload);
//       const saved: Credential = response.data;

//       toast({ title: "Credential saved", description: `${saved.credentialLabel} has been vaulted successfully.` });
//       onSuccess(saved);
//       onClose();
//     } catch (err: any) {
//       const msg = err?.response?.data?.message ?? err?.response?.data?.error ?? "Failed to save credential. Please check your inputs.";
//       toast({ title: "Save failed", description: msg, variant: "destructive" });
//     } finally {
//       setSubmitting(false);
//     }
//   }

//   return (
//     <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
//       <DialogContent className={cn("max-h-[90vh] overflow-y-auto border-slate-800 bg-slate-950 text-slate-200 shadow-2xl shadow-black/60 sm:max-w-lg")}>
//         <DialogHeader className="border-b border-slate-800/80 pb-4">
//           <div className="flex items-center gap-2.5">
//             <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-cyan-500/15 ring-1 ring-cyan-500/30">
//               <Key size={14} className="text-cyan-400" />
//             </div>
//             <div>
//               <DialogTitle className="font-mono text-sm tracking-wide text-slate-100">ADD CREDENTIAL</DialogTitle>
//               <DialogDescription className="font-mono text-[10px] text-slate-500">Encrypted with AES-256/GCM at rest</DialogDescription>
//             </div>
//           </div>
//         </DialogHeader>

//         <form onSubmit={handleSubmit} className="space-y-5 pt-2">
//           <div>
//             <p className="mb-2 font-mono text-[10px] font-semibold tracking-[0.15em] text-slate-500">STEP 1 — SELECT PROVIDER</p>
//             <div className="grid grid-cols-2 gap-2">
//               {(["AWS", "AZURE"] as Provider[]).map((p) => (
//                 <button key={p} type="button" onClick={() => { setProvider(p); setErrors({}); }} className={cn("flex flex-col items-center gap-2.5 rounded-xl border px-4 py-4 transition-all duration-200", provider === p ? p === "AWS" ? "border-orange-500/50 bg-orange-500/10 ring-1 ring-orange-500/20" : "border-sky-500/50 bg-sky-500/10 ring-1 ring-sky-500/20" : "border-slate-800 bg-slate-900/40 hover:border-slate-700 hover:bg-slate-900/70")}>
//                   <ProviderIcon provider={p} size={26} />
//                   <span className="font-mono text-xs font-semibold text-slate-300">{p === "AWS" ? "Amazon Web Services" : "Microsoft Azure"}</span>
//                 </button>
//               ))}
//             </div>
//           </div>

//           {provider && (
//             <div className="space-y-3">
//               <p className="font-mono text-[10px] font-semibold tracking-[0.15em] text-slate-500">STEP 2 — ENTER CREDENTIALS</p>

//               <TextField
//                 id="credentialLabel"
//                 label="Credential Label"
//                 value={provider === "AWS" ? awsForm.credentialLabel : azureForm.credentialLabel}
//                 onChange={(v) => provider === "AWS" ? patchAws("credentialLabel", v) : patchAzure("credentialLabel", v)}
//                 placeholder="e.g. Production AWS Account"
//                 error={errors.credentialLabel}
//               />

//               {provider === "AWS" && (
//                 <>
//                   <TextField id="accessKeyId" label="Access Key ID" value={awsForm.accessKeyId} onChange={(v) => patchAws("accessKeyId", v)} placeholder="AKIAIOSFODNN7EXAMPLE" hint="Starts with AKIA, ASIA, AROA, etc." error={errors.accessKeyId} />
//                   <SecretInput id="secretAccessKey" label="Secret Access Key" value={awsForm.secretAccessKey} onChange={(v) => patchAws("secretAccessKey", v)} placeholder="wJalrXUtnFEMI/K7MDENG/bPxRfi..." required error={errors.secretAccessKey} />

//                   <div className="space-y-1.5">
//                     <Label className="font-mono text-xs text-slate-400">Default Region <span className="text-cyan-400">*</span></Label>
//                     <Select value={awsForm.defaultRegion} onValueChange={(v) => patchAws("defaultRegion", v)}>
//                       <SelectTrigger className="border-slate-700 bg-slate-900/70 font-mono text-xs text-slate-200 focus:ring-cyan-500/20">
//                         <SelectValue placeholder="Select region" />
//                       </SelectTrigger>
//                       <SelectContent className="border-slate-700 bg-slate-900">
//                         {AWS_REGIONS.map((r) => (
//                           <SelectItem key={r.value} value={r.value} className="font-mono text-xs text-slate-300 focus:bg-slate-800 focus:text-slate-100">{r.label}</SelectItem>
//                         ))}
//                       </SelectContent>
//                     </Select>
//                     {errors.defaultRegion && <p className="flex items-center gap-1 font-mono text-[10px] text-red-400"><AlertCircle size={10} /> {errors.defaultRegion}</p>}
//                   </div>
//                 </>
//               )}

//               {provider === "AZURE" && (
//                 <>
//                   <TextField id="tenantId" label="Tenant ID" value={azureForm.tenantId} onChange={(v) => patchAzure("tenantId", v)} placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" hint="Your Azure AD Tenant (Directory) ID" error={errors.tenantId} />
//                   <TextField id="clientId" label="Client ID (App Registration)" value={azureForm.clientId} onChange={(v) => patchAzure("clientId", v)} placeholder="yyyyyyyy-yyyy-yyyy-yyyy-yyyyyyyyyyyy" error={errors.clientId} />
//                   <SecretInput id="clientSecret" label="Client Secret" value={azureForm.clientSecret} onChange={(v) => patchAzure("clientSecret", v)} placeholder="your~client~secret~value" required error={errors.clientSecret} />
//                   <TextField id="subscriptionId" label="Subscription ID" value={azureForm.subscriptionId} onChange={(v) => patchAzure("subscriptionId", v)} placeholder="zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz" error={errors.subscriptionId} />
//                 </>
//               )}
//             </div>
//           )}

//           {provider && (
//             <div className="flex items-start gap-2.5 rounded-lg border border-emerald-500/20 bg-emerald-500/5 px-3 py-2.5">
//               <ShieldCheck size={13} className="mt-0.5 shrink-0 text-emerald-400" />
//               <p className="font-mono text-[10px] text-emerald-400/80">
//                 All secrets are encrypted client-to-server over TLS and stored with AES-256/GCM encryption. Raw values are never returned.
//               </p>
//             </div>
//           )}

//           <div className="flex items-center justify-end gap-2.5 border-t border-slate-800/80 pt-4">
//             <Button type="button" variant="ghost" onClick={onClose} disabled={submitting} className="font-mono text-xs text-slate-400 hover:text-slate-200">Cancel</Button>
//             <Button type="submit" disabled={!provider || submitting} className={cn("font-mono text-xs bg-cyan-500/15 text-cyan-400 ring-1 ring-cyan-500/30 hover:bg-cyan-500/25 hover:ring-cyan-500/50 disabled:pointer-events-none disabled:opacity-40")}>
//               {submitting ? <><Loader2 size={12} className="mr-2 animate-spin" /> Encrypting & Saving...</> : <><Lock size={12} className="mr-2" /> Save to Vault</>}
//             </Button>
//           </div>
//         </form>
//       </DialogContent>
//     </Dialog>
//   );
// }

// // ── Page Header ──────────────────────────────────────────────────────────────

// function PageHeader({ count, loading, onRefresh, onAdd }: { count: number; loading: boolean; onRefresh: () => void; onAdd: () => void; }) {
//   return (
//     <div className="mb-8 flex items-start justify-between gap-4">
//       <div>
//         <div className="mb-1 flex items-center gap-2.5">
//           <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-500/15 ring-1 ring-cyan-500/30">
//             <Key size={16} className="text-cyan-400" />
//           </div>
//           <div>
//             <h1 className="font-mono text-xl font-bold tracking-tight text-slate-100">CREDENTIAL VAULT</h1>
//             <p className="font-mono text-[10px] tracking-widest text-slate-500">{count} STORED SECRET{count !== 1 ? "S" : ""} · AES-256/GCM ENCRYPTED</p>
//           </div>
//         </div>
//         <p className="mt-2 max-w-lg text-sm text-slate-500">Securely store and manage cloud provider credentials. Keys are encrypted at rest and decrypted only at execution time.</p>
//       </div>

//       <div className="flex shrink-0 items-center gap-2">
//         <button onClick={onRefresh} disabled={loading} className={cn("flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-800/60 px-3 py-2 font-mono text-xs text-slate-400 transition-all hover:border-slate-600 hover:text-slate-200 disabled:pointer-events-none disabled:opacity-40")}>
//           <RefreshCw size={13} className={loading ? "animate-spin" : ""} /> Refresh
//         </button>
//         <button onClick={onAdd} className={cn("flex items-center gap-2 rounded-lg bg-cyan-500/15 px-4 py-2 font-mono text-xs font-semibold text-cyan-400 ring-1 ring-cyan-500/30 transition-all hover:bg-cyan-500/25 hover:ring-cyan-500/50")}>
//           <Plus size={13} /> Add Credential
//         </button>
//       </div>
//     </div>
//   );
// }

// // ── Main Page ────────────────────────────────────────────────────────────────

// export default function CredentialsPage() {
//   const [credentials, setCredentials] = useState<Credential[]>([]);
//   const [loading, setLoading] = useState(true);
//   const [error, setError] = useState<string | null>(null);
//   const [modalOpen, setModalOpen] = useState(false);

//   const { toast } = useToast();

//   const fetchCredentials = useCallback(async () => {
//     setLoading(true);
//     setError(null);
//     try {
//       const res = await api.get("/credentials");
//       const list: Credential[] = Array.isArray(res.data) ? res.data : res.data.credentials ?? [];
//       setCredentials(list);
//     } catch (err: any) {
//       const msg = err?.response?.data?.message ?? "Failed to load credentials.";
//       setError(msg);
//       toast({ title: "Error", description: msg, variant: "destructive" });
//     } finally {
//       setLoading(false);
//     }
//   }, []);

//   useEffect(() => {
//     fetchCredentials();
//   }, [fetchCredentials]);

//   function handleCredentialAdded(newCred: Credential) {
//     setCredentials((prev) => [newCred, ...prev]);
//   }

//   function handleCredentialDeleted(id: string) {
//     setCredentials((prev) => prev.filter((c) => c.credentialId !== id));
//   }

//   return (
//     <>
//       <div className="min-h-[calc(100vh-4rem)]">
//         <PageHeader count={credentials.length} loading={loading} onRefresh={fetchCredentials} onAdd={() => setModalOpen(true)} />

//         {loading && (
//           <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
//             {[...Array(3)].map((_, i) => (
//               <div key={i} className="animate-pulse rounded-xl border border-slate-800 bg-slate-900/40 p-4" style={{ animationDelay: `${i * 100}ms` }}>
//                 <div className="mb-3 flex items-center gap-2.5">
//                   <div className="h-9 w-9 rounded-lg bg-slate-800" />
//                   <div className="space-y-1.5">
//                     <div className="h-3.5 w-32 rounded bg-slate-800" />
//                     <div className="h-2.5 w-16 rounded bg-slate-800/60" />
//                   </div>
//                 </div>
//                 <div className="space-y-2 rounded-lg bg-slate-950/40 p-3">
//                   <div className="h-2.5 w-full rounded bg-slate-800/60" />
//                   <div className="h-2.5 w-4/5 rounded bg-slate-800/60" />
//                   <div className="h-2.5 w-3/4 rounded bg-slate-800/60" />
//                 </div>
//               </div>
//             ))}
//           </div>
//         )}

//         {!loading && error && (
//           <div className="flex flex-col items-center rounded-xl border border-red-500/20 bg-red-500/5 px-6 py-12 text-center">
//             <AlertCircle size={32} className="mb-3 text-red-400" />
//             <p className="font-mono text-sm text-red-400">{error}</p>
//             <button onClick={fetchCredentials} className="mt-4 font-mono text-xs text-slate-500 underline underline-offset-4 hover:text-slate-300">Try again</button>
//           </div>
//         )}

//         {!loading && !error && credentials.length === 0 && (
//           <EmptyState onAdd={() => setModalOpen(true)} />
//         )}

//         {!loading && !error && credentials.length > 0 && (
//           <>
//             <div className="mb-5 flex items-center gap-4">
//               {(["AWS", "AZURE"] as Provider[]).map((p) => {
//                 const count = credentials.filter((c) => c.provider === p).length;
//                 if (count === 0) return null;
//                 return (
//                   <div key={p} className="flex items-center gap-2 rounded-lg border border-slate-800 bg-slate-900/40 px-3 py-1.5">
//                     <ProviderIcon provider={p} size={13} />
//                     <span className="font-mono text-xs text-slate-400">{count} {p}</span>
//                   </div>
//                 );
//               })}
//             </div>

//             <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
//               {credentials.map((cred) => (
//                 <CredentialCard key={cred.credentialId} credential={cred} onDelete={handleCredentialDeleted} />
//               ))}
//             </div>
//           </>
//         )}
//       </div>

//       <AddCredentialModal open={modalOpen} onClose={() => setModalOpen(false)} onSuccess={handleCredentialAdded} />
//       <Toaster />
//     </>
//   );
// }


"use client";

import { useState, useEffect, useCallback, memo } from "react";
import {
  Key,
  Plus,
  Cloud,
  Server,
  Trash2,
  RefreshCw,
  ShieldCheck,
  AlertCircle,
  Copy,
  Check,
  ChevronDown,
  Loader2,
  Lock,
  Eye,
  EyeOff,
} from "lucide-react";
import { cn } from "@/lib/utils";
import api from "@/lib/api";

// ── Shadcn UI imports ────────────────────────────────────────────────────────
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useToast } from "@/components/ui/use-toast";
import { Toaster } from "@/components/ui/toaster";

// ── Types ────────────────────────────────────────────────────────────────────

type Provider = "AWS" | "AZURE";

interface Credential {
  credentialId: string;
  provider: Provider;
  credentialLabel: string;
  validationStatus: "VALID" | "INVALID" | "UNVERIFIED";
  createdAt: string;
  accessKeyIdMasked?: string;
  defaultRegion?: string;
  clientIdMasked?: string;
  subscriptionIdMasked?: string;
}

interface AwsFormData {
  credentialLabel: string;
  accessKeyId: string;
  secretAccessKey: string;
  defaultRegion: string;
}

interface AzureFormData {
  credentialLabel: string;
  tenantId: string;
  clientId: string;
  clientSecret: string;
  subscriptionId: string;
}

// ── Constants ────────────────────────────────────────────────────────────────

const AWS_REGIONS = [
  { value: "us-east-1", label: "US East (N. Virginia)" },
  { value: "us-east-2", label: "US East (Ohio)" },
  { value: "us-west-1", label: "US West (N. California)" },
  { value: "us-west-2", label: "US West (Oregon)" },
  { value: "ap-south-1", label: "Asia Pacific (Mumbai)" },
  { value: "ap-southeast-1", label: "Asia Pacific (Singapore)" },
  { value: "ap-southeast-2", label: "Asia Pacific (Sydney)" },
  { value: "ap-northeast-1", label: "Asia Pacific (Tokyo)" },
  { value: "eu-west-1", label: "Europe (Ireland)" },
  { value: "eu-west-2", label: "Europe (London)" },
  { value: "eu-central-1", label: "Europe (Frankfurt)" },
];

const INITIAL_AWS: AwsFormData = {
  credentialLabel: "",
  accessKeyId: "",
  secretAccessKey: "",
  defaultRegion: "us-east-1",
};

const INITIAL_AZURE: AzureFormData = {
  credentialLabel: "",
  tenantId: "",
  clientId: "",
  clientSecret: "",
  subscriptionId: "",
};

// ── Helper sub-components ────────────────────────────────────────────────────

function ProviderIcon({ provider, size = 18, className }: { provider: Provider; size?: number; className?: string; }) {
  if (provider === "AWS") {
    return (
      <svg width={size} height={size} viewBox="0 0 24 24" fill="none" className={className}>
        <path d="M6.76 10.45c0 .28.03.5.08.67.06.16.14.34.25.53a.32.32 0 0 1 .05.17c0 .07-.04.15-.13.22l-.44.3a.32.32 0 0 1-.18.06c-.07 0-.14-.03-.2-.1a2.1 2.1 0 0 1-.25-.32 5.4 5.4 0 0 1-.21-.4c-.54.63-1.21.94-2.02.94-.58 0-1.04-.16-1.38-.5-.34-.32-.51-.75-.51-1.29 0-.57.2-1.03.61-1.37.41-.35.95-.52 1.64-.52.23 0 .46.02.7.05.25.04.5.09.77.15v-.49c0-.51-.1-.87-.32-1.08-.21-.22-.57-.32-1.09-.32-.23 0-.47.03-.71.08-.24.06-.48.13-.71.23a1.9 1.9 0 0 1-.22.08.39.39 0 0 1-.1.02c-.09 0-.13-.06-.13-.19v-.3c0-.1.01-.17.04-.22a.43.43 0 0 1 .17-.13c.23-.12.51-.22.83-.3.33-.09.67-.13 1.04-.13.79 0 1.37.18 1.74.54.37.36.55.9.55 1.63v2.15zm-2.8.96c.21 0 .43-.04.66-.12.23-.08.43-.23.61-.43.1-.12.18-.25.22-.4.04-.14.07-.32.07-.53v-.25a5.46 5.46 0 0 0-1.28-.15c-.46 0-.8.09-1.02.28-.23.19-.34.45-.34.8 0 .32.08.56.25.73.16.18.4.07.83.07zm6.93.93c-.11 0-.18-.02-.23-.06-.05-.04-.1-.13-.14-.25L8.67 5.86a1.1 1.1 0 0 1-.06-.26c0-.1.05-.16.15-.16h.62c.12 0 .2.02.24.06.05.04.09.13.13.25l1.36 5.37 1.27-5.37c.03-.13.07-.21.12-.25.05-.04.14-.06.25-.06h.5c.12 0 .2.02.25.06.05.04.1.13.12.25l1.28 5.44 1.4-5.44c.04-.13.08-.21.13-.25.05-.04.12-.06.24-.06h.59c.1 0 .15.05.15.16 0 .03 0 .07-.01.11l-.05.15-1.91 6.17c-.04.13-.08.21-.13.25-.05.04-.13.06-.23.06h-.54c-.12 0-.2-.02-.25-.06-.05-.05-.09-.13-.12-.26l-1.26-5.2-1.26 5.2c-.03.13-.07.21-.12.26-.05.04-.14.06-.25.06h-.55zm10.2.26c-.33 0-.66-.04-.97-.12a2.8 2.8 0 0 1-.74-.28.44.44 0 0 1-.18-.2.5.5 0 0 1-.03-.2v-.31c0-.13.05-.2.14-.2.04 0 .07.01.11.02.04.02.09.04.14.07.2.09.4.15.62.2.22.04.44.07.66.07.35 0 .62-.06.81-.19.2-.12.3-.31.3-.55 0-.16-.05-.3-.16-.41-.11-.11-.3-.21-.59-.31l-.84-.26c-.42-.14-.73-.33-.93-.6a1.43 1.43 0 0 1-.3-.87c0-.25.05-.48.16-.67.11-.2.25-.37.44-.5.18-.14.39-.25.63-.32.24-.07.49-.1.76-.1.13 0 .27.01.4.03.14.02.27.04.4.07.12.03.24.06.35.1.11.04.2.08.26.12.1.06.17.12.21.19a.5.5 0 0 1 .05.23v.29c0 .13-.05.2-.14.2a.59.59 0 0 1-.22-.07 2.6 2.6 0 0 0-1.1-.23c-.32 0-.57.05-.74.16-.18.11-.27.28-.27.52 0 .16.06.3.17.41.12.11.33.22.64.32l.82.26c.42.13.72.32.9.57.19.25.28.53.28.84 0 .26-.05.49-.15.7-.1.2-.25.38-.44.53-.19.14-.42.25-.68.33-.27.07-.55.11-.85.11z" fill="#FF9900" />
      </svg>
    );
  }
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" className={className}>
      <path d="M13.05 4.24L6.56 18.05l4.73.83L22 4.24H13.05zM12.1 5.88L8.03 17.45l-5.53.96L8.24 9.5l3.86-3.62z" fill="#0089D6" />
    </svg>
  );
}

function ValidationBadge({ status }: { status: Credential["validationStatus"] }) {
  const map = {
    VALID: { label: "Valid", class: "bg-emerald-500/15 text-emerald-400 border-emerald-500/30" },
    INVALID: { label: "Invalid", class: "bg-red-500/15 text-red-400 border-red-500/30" },
    UNVERIFIED: { label: "Unverified", class: "bg-amber-500/15 text-amber-400 border-amber-500/30" },
  };
  const config = map[status] || map.UNVERIFIED;
  return (
    <span className={cn("rounded-full border px-2 py-0.5 font-mono text-[10px] font-semibold tracking-wider", config.class)}>
      {config.label}
    </span>
  );
}

function CopyButton({ value }: { value: string }) {
  const [copied, setCopied] = useState(false);
  const handleCopy = async () => {
    await navigator.clipboard.writeText(value);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };
  return (
    <button onClick={handleCopy} className="rounded p-1 text-slate-600 transition-colors hover:bg-slate-700/50 hover:text-slate-300" title="Copy ID">
      {copied ? <Check size={12} className="text-emerald-400" /> : <Copy size={12} />}
    </button>
  );
}

// ── FIXED: Extracted Components ──────────────────────────────────────────────

const TextField = memo(function TextField({
  id,
  label,
  value,
  onChange,
  placeholder,
  hint,
  error
}: {
  id: string;
  label: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  hint?: string;
  error?: string;
}) {
  return (
    <div className="space-y-1.5">
      <Label htmlFor={id} className="font-mono text-xs text-slate-400">
        {label} <span className="text-cyan-400">*</span>
      </Label>
      <Input
        id={id}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className={cn(
          "border-slate-700 bg-slate-900/70 font-mono text-xs text-slate-200",
          "placeholder:text-slate-600 focus:border-cyan-500/50 focus:ring-cyan-500/20",
          error && "border-red-500/60"
        )}
      />
      {hint && !error && (
        <p className="font-mono text-[10px] text-slate-600">{hint}</p>
      )}
      {error && (
        <p className="flex items-center gap-1 font-mono text-[10px] text-red-400">
          <AlertCircle size={10} /> {error}
        </p>
      )}
    </div>
  );
});

const SecretInput = memo(function SecretInput({
  id,
  label,
  value,
  onChange,
  placeholder,
  required,
  error
}: {
  id: string;
  label: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  required?: boolean;
  error?: string;
}) {
  const [show, setShow] = useState(false);
  return (
    <div className="space-y-1.5">
      <Label htmlFor={id} className="font-mono text-xs text-slate-400">
        {label}
        {required && <span className="ml-1 text-cyan-400">*</span>}
      </Label>
      <div className="relative">
        <Input
          id={id}
          type={show ? "text" : "password"}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          required={required}
          className={cn(
            "border-slate-700 bg-slate-900/70 pr-9 font-mono text-xs text-slate-200 placeholder:text-slate-600 focus:border-cyan-500/50 focus:ring-cyan-500/20",
            error && "border-red-500/60"
          )}
        />
        <button
          type="button"
          onClick={() => setShow((s) => !s)}
          className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300"
        >
          {show ? <EyeOff size={13} /> : <Eye size={13} />}
        </button>
      </div>
      {error && (
        <p className="flex items-center gap-1 font-mono text-[10px] text-red-400 mt-1">
          <AlertCircle size={10} /> {error}
        </p>
      )}
    </div>
  );
});

// ── Credential Card ──────────────────────────────────────────────────────────

function CredentialCard({ credential, onDelete }: { credential: Credential; onDelete: (id: string) => void; }) {
  const [deleting, setDeleting] = useState(false);
  const { toast } = useToast();

  async function handleDelete() {
    if (!credential.credentialId) return;
    setDeleting(true);
    try {
      await api.delete(`/credentials/${credential.credentialId}`);
      toast({ title: "Credential removed", description: credential.credentialLabel });
      onDelete(credential.credentialId);
    } catch {
      toast({ title: "Delete failed", description: "Could not remove the credential.", variant: "destructive" });
    } finally {
      setDeleting(false);
    }
  }

  const metaLines = credential.provider === "AWS"
      ? [
        { key: "Key ID", val: credential.accessKeyIdMasked ?? "—" },
        { key: "Region", val: credential.defaultRegion ?? "—" },
      ]
      : [
        { key: "Client ID", val: credential.clientIdMasked ?? "—" },
        { key: "Subscription", val: credential.subscriptionIdMasked ?? "—" },
      ];

  return (
    <div className={cn("group relative overflow-hidden rounded-xl border border-slate-800/80 bg-slate-900/40 backdrop-blur-sm transition-all duration-300 hover:border-slate-700/80 hover:bg-slate-900/60 hover:shadow-lg hover:shadow-slate-950/50")}>
      <div className={cn("absolute inset-x-0 top-0 h-px", credential.provider === "AWS" ? "bg-gradient-to-r from-transparent via-orange-400/40 to-transparent" : "bg-gradient-to-r from-transparent via-sky-400/40 to-transparent")} />
      <div className="p-4">
        <div className="mb-3 flex items-start justify-between gap-2">
          <div className="flex items-center gap-2.5">
            <div className={cn("flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ring-1", credential.provider === "AWS" ? "bg-orange-500/10 ring-orange-500/25" : "bg-sky-500/10 ring-sky-500/25")}>
              <ProviderIcon provider={credential.provider} size={18} />
            </div>
            <div>
              <p className="font-semibold text-slate-100 leading-tight">{credential.credentialLabel}</p>
              <p className="font-mono text-[10px] text-slate-500 mt-0.5">{credential.provider}</p>
            </div>
          </div>
          <ValidationBadge status={credential.validationStatus} />
        </div>
        <div className="mb-3 space-y-1.5 rounded-lg bg-slate-950/40 px-3 py-2">
          {metaLines.map((m) => (
            <div key={m.key} className="flex items-center justify-between">
              <span className="font-mono text-[10px] text-slate-600">{m.key}</span>
              <span className="font-mono text-[10px] text-slate-400">{m.val}</span>
            </div>
          ))}
          <div className="flex items-center justify-between">
            <span className="font-mono text-[10px] text-slate-600">ID</span>
            <div className="flex items-center gap-1">
              <span className="font-mono text-[10px] text-slate-400">{credential.credentialId || "Unknown"}</span>
              <CopyButton value={credential.credentialId || ""} />
            </div>
          </div>
        </div>
        <div className="flex items-center justify-between">
          <span className="font-mono text-[10px] text-slate-600">Added {credential.createdAt ? new Date(credential.createdAt).toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric" }) : "—"}</span>
          <button onClick={handleDelete} disabled={deleting} className={cn("flex items-center gap-1.5 rounded-md px-2.5 py-1.5 font-mono text-[10px] text-slate-600 transition-all duration-200 hover:bg-red-500/10 hover:text-red-400 disabled:pointer-events-none disabled:opacity-40")}>
            {deleting ? <Loader2 size={11} className="animate-spin" /> : <Trash2 size={11} />}
            Remove
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Empty State ──────────────────────────────────────────────────────────────

function EmptyState({ onAdd }: { onAdd: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-slate-800 bg-slate-900/20 px-6 py-20 text-center">
      <div className="mb-5 flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-800/60 ring-1 ring-slate-700/50">
        <Lock size={28} className="text-slate-500" />
      </div>
      <p className="mb-1 text-base font-semibold text-slate-300">No credentials vaulted</p>
      <p className="mb-6 max-w-xs text-sm text-slate-500">Securely store your AWS IAM keys or Azure Service Principal credentials. All secrets are encrypted at rest with AES-256/GCM.</p>
      <button onClick={onAdd} className={cn("flex items-center gap-2 rounded-lg bg-cyan-500/15 px-4 py-2.5 font-medium text-sm text-cyan-400 ring-1 ring-cyan-500/30 transition-all hover:bg-cyan-500/25 hover:ring-cyan-500/50")}>
        <Plus size={15} /> Add your first credential
      </button>
    </div>
  );
}

// ── Add Credential Modal ─────────────────────────────────────────────────────

function AddCredentialModal({ open, onClose, onSuccess }: { open: boolean; onClose: () => void; onSuccess: (cred: Credential) => void; }) {
  const { toast } = useToast();
  const [provider, setProvider] = useState<Provider | "">("");
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [awsForm, setAwsForm] = useState<AwsFormData>(INITIAL_AWS);
  const [azureForm, setAzureForm] = useState<AzureFormData>(INITIAL_AZURE);

  useEffect(() => {
    if (!open) {
      setProvider("");
      setAwsForm(INITIAL_AWS);
      setAzureForm(INITIAL_AZURE);
      setErrors({});
      setSubmitting(false);
    }
  }, [open]);

  const patchAws = useCallback((key: keyof AwsFormData, val: string) => {
    setAwsForm((prev) => ({ ...prev, [key]: val }));
    setErrors((prev) => { const n = { ...prev }; delete n[key]; return n; });
  }, []);

  const patchAzure = useCallback((key: keyof AzureFormData, val: string) => {
    setAzureForm((prev) => ({ ...prev, [key]: val }));
    setErrors((prev) => { const n = { ...prev }; delete n[key]; return n; });
  }, []);

  function validate(): boolean {
    const errs: Record<string, string> = {};
    if (provider === "AWS") {
      if (!awsForm.credentialLabel || !awsForm.credentialLabel.trim()) errs.credentialLabel = "Label is required.";
      if (!awsForm.accessKeyId || !awsForm.accessKeyId.trim()) errs.accessKeyId = "Access Key ID is required.";
      if (!awsForm.secretAccessKey || !awsForm.secretAccessKey.trim()) errs.secretAccessKey = "Secret Access Key is required.";
      if (!awsForm.defaultRegion) errs.defaultRegion = "Region is required.";
    } else if (provider === "AZURE") {
      if (!azureForm.credentialLabel || !azureForm.credentialLabel.trim()) errs.credentialLabel = "Label is required.";
      if (!azureForm.tenantId || !azureForm.tenantId.trim()) errs.tenantId = "Tenant ID is required.";
      if (!azureForm.clientId || !azureForm.clientId.trim()) errs.clientId = "Client ID is required.";
      if (!azureForm.clientSecret || !azureForm.clientSecret.trim()) errs.clientSecret = "Client Secret is required.";
      if (!azureForm.subscriptionId || !azureForm.subscriptionId.trim()) errs.subscriptionId = "Subscription ID is required.";
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  }

  // FIXED: Converted to a manual click handler to avoid form suppression
  async function handleManualSubmit(e: React.MouseEvent) {
    e.preventDefault();
    if (!provider) return;

    if (!validate()) {
      alert("Please fill in all required fields marked in red.");
      return;
    }

    setSubmitting(true);
    try {
      // NOTE: If this fails with a 404, check your Java backend. It might expect "/credentials" instead of "/credentials/aws"
      const endpoint = provider === "AWS" ? "/credentials/aws" : "/credentials/azure";
      const payload = provider === "AWS" ? awsForm : azureForm;

      console.log("Sending Payload to:", endpoint, payload);

      const response = await api.post(endpoint, payload);
      const saved: Credential = response.data;

      toast({ title: "Credential saved", description: `${saved.credentialLabel} has been vaulted successfully.` });
      onSuccess(saved);
      onClose();
    } catch (err: any) {
      console.error("API Error Response:", err.response || err);
      const msg = err?.response?.data?.message ?? err?.response?.data?.error ?? "Failed to save credential. Check browser console for details.";
      
      // FIXED: Using alert to ensure visibility even if toast is hidden behind modal
      alert(`Execution Failed:\n\n${msg}\n\n(If this is a 404, the backend URL in api.post might need adjustment)`);
      toast({ title: "Save failed", description: msg, variant: "destructive" });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className={cn("max-h-[90vh] overflow-y-auto border-slate-800 bg-slate-950 text-slate-200 shadow-2xl shadow-black/60 sm:max-w-lg")}>
        <DialogHeader className="border-b border-slate-800/80 pb-4">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-cyan-500/15 ring-1 ring-cyan-500/30">
              <Key size={14} className="text-cyan-400" />
            </div>
            <div>
              <DialogTitle className="font-mono text-sm tracking-wide text-slate-100">ADD CREDENTIAL</DialogTitle>
              <DialogDescription className="font-mono text-[10px] text-slate-500">Encrypted with AES-256/GCM at rest</DialogDescription>
            </div>
          </div>
        </DialogHeader>

        {/* FIXED: Removed onSubmit from form. Relies on button onClick. */}
        <div className="space-y-5 pt-2">
          <div>
            <p className="mb-2 font-mono text-[10px] font-semibold tracking-[0.15em] text-slate-500">STEP 1 — SELECT PROVIDER</p>
            <div className="grid grid-cols-2 gap-2">
              {(["AWS", "AZURE"] as Provider[]).map((p) => (
                <button key={p} type="button" onClick={() => { setProvider(p); setErrors({}); }} className={cn("flex flex-col items-center gap-2.5 rounded-xl border px-4 py-4 transition-all duration-200", provider === p ? p === "AWS" ? "border-orange-500/50 bg-orange-500/10 ring-1 ring-orange-500/20" : "border-sky-500/50 bg-sky-500/10 ring-1 ring-sky-500/20" : "border-slate-800 bg-slate-900/40 hover:border-slate-700 hover:bg-slate-900/70")}>
                  <ProviderIcon provider={p} size={26} />
                  <span className="font-mono text-xs font-semibold text-slate-300">{p === "AWS" ? "Amazon Web Services" : "Microsoft Azure"}</span>
                </button>
              ))}
            </div>
          </div>

          {provider && (
            <div className="space-y-3">
              <p className="font-mono text-[10px] font-semibold tracking-[0.15em] text-slate-500">STEP 2 — ENTER CREDENTIALS</p>

              <TextField
                id="credentialLabel"
                label="Credential Label"
                value={provider === "AWS" ? awsForm.credentialLabel : azureForm.credentialLabel}
                onChange={(v) => provider === "AWS" ? patchAws("credentialLabel", v) : patchAzure("credentialLabel", v)}
                placeholder="e.g. Production AWS Account"
                error={errors.credentialLabel}
              />

              {provider === "AWS" && (
                <>
                  <TextField id="accessKeyId" label="Access Key ID" value={awsForm.accessKeyId} onChange={(v) => patchAws("accessKeyId", v)} placeholder="AKIAIOSFODNN7EXAMPLE" hint="Starts with AKIA, ASIA, AROA, etc." error={errors.accessKeyId} />
                  <SecretInput id="secretAccessKey" label="Secret Access Key" value={awsForm.secretAccessKey} onChange={(v) => patchAws("secretAccessKey", v)} placeholder="wJalrXUtnFEMI/K7MDENG/bPxRfi..." required error={errors.secretAccessKey} />

                  <div className="space-y-1.5">
                    <Label className="font-mono text-xs text-slate-400">Default Region <span className="text-cyan-400">*</span></Label>
                    <Select value={awsForm.defaultRegion} onValueChange={(v) => patchAws("defaultRegion", v)}>
                      <SelectTrigger className="border-slate-700 bg-slate-900/70 font-mono text-xs text-slate-200 focus:ring-cyan-500/20">
                        <SelectValue placeholder="Select region" />
                      </SelectTrigger>
                      <SelectContent className="border-slate-700 bg-slate-900">
                        {AWS_REGIONS.map((r) => (
                          <SelectItem key={r.value} value={r.value} className="font-mono text-xs text-slate-300 focus:bg-slate-800 focus:text-slate-100">{r.label}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    {errors.defaultRegion && <p className="flex items-center gap-1 font-mono text-[10px] text-red-400"><AlertCircle size={10} /> {errors.defaultRegion}</p>}
                  </div>
                </>
              )}

              {provider === "AZURE" && (
                <>
                  <TextField id="tenantId" label="Tenant ID" value={azureForm.tenantId} onChange={(v) => patchAzure("tenantId", v)} placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" hint="Your Azure AD Tenant (Directory) ID" error={errors.tenantId} />
                  <TextField id="clientId" label="Client ID (App Registration)" value={azureForm.clientId} onChange={(v) => patchAzure("clientId", v)} placeholder="yyyyyyyy-yyyy-yyyy-yyyy-yyyyyyyyyyyy" error={errors.clientId} />
                  <SecretInput id="clientSecret" label="Client Secret" value={azureForm.clientSecret} onChange={(v) => patchAzure("clientSecret", v)} placeholder="your~client~secret~value" required error={errors.clientSecret} />
                  <TextField id="subscriptionId" label="Subscription ID" value={azureForm.subscriptionId} onChange={(v) => patchAzure("subscriptionId", v)} placeholder="zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz" error={errors.subscriptionId} />
                </>
              )}
            </div>
          )}

          {provider && (
            <div className="flex items-start gap-2.5 rounded-lg border border-emerald-500/20 bg-emerald-500/5 px-3 py-2.5">
              <ShieldCheck size={13} className="mt-0.5 shrink-0 text-emerald-400" />
              <p className="font-mono text-[10px] text-emerald-400/80">
                All secrets are encrypted client-to-server over TLS and stored with AES-256/GCM encryption. Raw values are never returned.
              </p>
            </div>
          )}

          <div className="flex items-center justify-end gap-2.5 border-t border-slate-800/80 pt-4">
            <Button type="button" variant="ghost" onClick={onClose} disabled={submitting} className="font-mono text-xs text-slate-400 hover:text-slate-200">Cancel</Button>
            <Button 
              type="button" 
              onClick={handleManualSubmit}
              disabled={!provider || submitting} 
              className={cn("font-mono text-xs bg-cyan-500/15 text-cyan-400 ring-1 ring-cyan-500/30 hover:bg-cyan-500/25 hover:ring-cyan-500/50 disabled:pointer-events-none disabled:opacity-40")}
            >
              {submitting ? <><Loader2 size={12} className="mr-2 animate-spin" /> Encrypting & Saving...</> : <><Lock size={12} className="mr-2" /> Save to Vault</>}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}

// ── Page Header ──────────────────────────────────────────────────────────────

function PageHeader({ count, loading, onRefresh, onAdd }: { count: number; loading: boolean; onRefresh: () => void; onAdd: () => void; }) {
  return (
    <div className="mb-8 flex items-start justify-between gap-4">
      <div>
        <div className="mb-1 flex items-center gap-2.5">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-500/15 ring-1 ring-cyan-500/30">
            <Key size={16} className="text-cyan-400" />
          </div>
          <div>
            <h1 className="font-mono text-xl font-bold tracking-tight text-slate-100">CREDENTIAL VAULT</h1>
            <p className="font-mono text-[10px] tracking-widest text-slate-500">{count} STORED SECRET{count !== 1 ? "S" : ""} · AES-256/GCM ENCRYPTED</p>
          </div>
        </div>
        <p className="mt-2 max-w-lg text-sm text-slate-500">Securely store and manage cloud provider credentials. Keys are encrypted at rest and decrypted only at execution time.</p>
      </div>

      <div className="flex shrink-0 items-center gap-2">
        <button onClick={onRefresh} disabled={loading} className={cn("flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-800/60 px-3 py-2 font-mono text-xs text-slate-400 transition-all hover:border-slate-600 hover:text-slate-200 disabled:pointer-events-none disabled:opacity-40")}>
          <RefreshCw size={13} className={loading ? "animate-spin" : ""} /> Refresh
        </button>
        <button onClick={onAdd} className={cn("flex items-center gap-2 rounded-lg bg-cyan-500/15 px-4 py-2 font-mono text-xs font-semibold text-cyan-400 ring-1 ring-cyan-500/30 transition-all hover:bg-cyan-500/25 hover:ring-cyan-500/50")}>
          <Plus size={13} /> Add Credential
        </button>
      </div>
    </div>
  );
}

// ── Main Page ────────────────────────────────────────────────────────────────

export default function CredentialsPage() {
  const [credentials, setCredentials] = useState<Credential[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  const { toast } = useToast();

  const fetchCredentials = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get("/credentials");
      const list: Credential[] = Array.isArray(res.data) ? res.data : res.data.credentials ?? [];
      setCredentials(list);
    } catch (err: any) {
      const msg = err?.response?.data?.message ?? "Failed to load credentials.";
      setError(msg);
      toast({ title: "Error", description: msg, variant: "destructive" });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchCredentials();
  }, [fetchCredentials]);

  function handleCredentialAdded(newCred: Credential) {
    setCredentials((prev) => [newCred, ...prev]);
  }

  function handleCredentialDeleted(id: string) {
    setCredentials((prev) => prev.filter((c) => c.credentialId !== id));
  }

  return (
    <>
      <div className="min-h-[calc(100vh-4rem)]">
        <PageHeader count={credentials.length} loading={loading} onRefresh={fetchCredentials} onAdd={() => setModalOpen(true)} />

        {loading && (
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="animate-pulse rounded-xl border border-slate-800 bg-slate-900/40 p-4" style={{ animationDelay: `${i * 100}ms` }}>
                <div className="mb-3 flex items-center gap-2.5">
                  <div className="h-9 w-9 rounded-lg bg-slate-800" />
                  <div className="space-y-1.5">
                    <div className="h-3.5 w-32 rounded bg-slate-800" />
                    <div className="h-2.5 w-16 rounded bg-slate-800/60" />
                  </div>
                </div>
                <div className="space-y-2 rounded-lg bg-slate-950/40 p-3">
                  <div className="h-2.5 w-full rounded bg-slate-800/60" />
                  <div className="h-2.5 w-4/5 rounded bg-slate-800/60" />
                  <div className="h-2.5 w-3/4 rounded bg-slate-800/60" />
                </div>
              </div>
            ))}
          </div>
        )}

        {!loading && error && (
          <div className="flex flex-col items-center rounded-xl border border-red-500/20 bg-red-500/5 px-6 py-12 text-center">
            <AlertCircle size={32} className="mb-3 text-red-400" />
            <p className="font-mono text-sm text-red-400">{error}</p>
            <button onClick={fetchCredentials} className="mt-4 font-mono text-xs text-slate-500 underline underline-offset-4 hover:text-slate-300">Try again</button>
          </div>
        )}

        {!loading && !error && credentials.length === 0 && (
          <EmptyState onAdd={() => setModalOpen(true)} />
        )}

        {!loading && !error && credentials.length > 0 && (
          <>
            <div className="mb-5 flex items-center gap-4">
              {(["AWS", "AZURE"] as Provider[]).map((p) => {
                const count = credentials.filter((c) => c.provider === p).length;
                if (count === 0) return null;
                return (
                  <div key={p} className="flex items-center gap-2 rounded-lg border border-slate-800 bg-slate-900/40 px-3 py-1.5">
                    <ProviderIcon provider={p} size={13} />
                    <span className="font-mono text-xs text-slate-400">{count} {p}</span>
                  </div>
                );
              })}
            </div>

            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {credentials.map((cred) => (
                <CredentialCard key={cred.credentialId} credential={cred} onDelete={handleCredentialDeleted} />
              ))}
            </div>
          </>
        )}
      </div>

      <AddCredentialModal open={modalOpen} onClose={() => setModalOpen(false)} onSuccess={handleCredentialAdded} />
      <Toaster />
    </>
  );
}