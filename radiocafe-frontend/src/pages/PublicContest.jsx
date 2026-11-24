import { useAuth } from '../context/useAuth';
import useAction from '../lib/useAction';
import Layout from '../components/Layout';
import Contest from '../components/Contest';
export default function PublicContest() {const {user}=useAuth();const {action,busy,error,message}=useAction();return <Layout><main className="container page">{error&&<p className="notice error" role="alert">{error}</p>}{message&&<p className="notice success" role="status">{message}</p>}<Contest user={user} action={action} busy={busy}/></main></Layout>;}
