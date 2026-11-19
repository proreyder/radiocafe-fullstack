import { useState } from 'react';
export default function useAction() {
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  async function action(fn, success = '') {
    setBusy(true); setError(''); setMessage('');
    try { await fn(); setMessage(success); return true; }
    catch (e) { setError(e.message); return false; }
    finally { setBusy(false); }
  }
  return { busy, message, error, action };
}
