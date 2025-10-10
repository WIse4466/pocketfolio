import './App.css';
import { CategoryPage } from './pages/CategoryPage';
import { AccountPage } from './pages/AccountPage';
import { TransactionsPage } from './pages/TransactionsPage';
import { TransactionsCalendarPage } from './pages/TransactionsCalendarPage';
import { useState, useEffect } from 'react';
import { StatementsPage } from './pages/StatementsPage';
import { ImportPage } from './pages/ImportPage';
import { OverviewPage } from './pages/OverviewPage';
import { RecurrencesPage } from './pages/RecurrencesPage';
import { API_BASE } from './lib/api';

function App() {
  const [currentPage, setCurrentPage] = useState(window.location.hash);

  useEffect(() => {
    const handleHashChange = () => {
      setCurrentPage(window.location.hash);
    };
    window.addEventListener('hashchange', handleHashChange);
    return () => {
      window.removeEventListener('hashchange', handleHashChange);
    };
  }, []);

  let PageComponent;
  switch (currentPage) {
    case '#/statements':
      PageComponent = StatementsPage;
      break;
    case '#/import':
      PageComponent = ImportPage;
      break;
    case '#/overview':
      PageComponent = OverviewPage;
      break;
    case '#/recurrences':
      PageComponent = RecurrencesPage;
      break;
    case '#/calendar':
      PageComponent = TransactionsCalendarPage;
      break;
    case '#/transactions':
      PageComponent = TransactionsPage;
      break;
    case '#/accounts':
      PageComponent = AccountPage;
      break;
    case '#/categories':
    default:
      PageComponent = CategoryPage;
      break;
  }

  return (
    <div className="App">
      <nav>
        <a href="#/categories">分類管理</a> |
        <a href="#/accounts">帳戶管理</a> |
        <a href="#/transactions">交易</a> |
        <a href="#/calendar">月曆</a> |
        <a href="#/statements">帳單</a> |
        <a href="#/overview">概覽</a> |
        <a href="#/recurrences">排程</a> |
        <a href="#/import">匯入</a> |
        <a href={`${API_BASE}/api/exports/csv?v=1`} target="_blank" rel="noreferrer">下載 CSV</a>
      </nav>
      <hr />
      <PageComponent />
    </div>
  );
}

export default App;
