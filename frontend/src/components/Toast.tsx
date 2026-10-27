import { createContext, useCallback, useContext, useState, type ReactNode } from "react";
import { CheckCircle2, CircleAlert, X } from "lucide-react";
type ToastMessage = { id: number; text: string; type: "success" | "error" };
const ToastContext = createContext<(text: string, type?: ToastMessage["type"]) => void>(() => undefined);
export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastMessage[]>([]);
  const toast = useCallback((text: string, type: ToastMessage["type"] = "success") => {
    const id = Date.now() + Math.random();
    setItems((current) => [...current, { id, text, type }]);
    window.setTimeout(() => setItems((current) => current.filter((item) => item.id !== id)), 4200);
  }, []);
  return <ToastContext.Provider value={toast}>
    {children}
    <div className="toast-stack" aria-live="polite">{items.map((item) => <div className={`toast ${item.type}`} key={item.id}>
      {item.type === "success" ? <CheckCircle2 size={18} /> : <CircleAlert size={18} />}<span>{item.text}</span>
      <button aria-label="Đóng thông báo" onClick={() => setItems((current) => current.filter((entry) => entry.id !== item.id))}><X size={16} /></button>
    </div>)}</div>
  </ToastContext.Provider>;
}
export function useToast() { return useContext(ToastContext); }
