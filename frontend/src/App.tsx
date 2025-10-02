import './App.css';
import { CategoryPage } from './pages/CategoryPage';
import { AccountPage } from './pages/AccountPage';
import { TransactionsPage } from './pages/TransactionsPage';
import { useState, useEffect } from 'react';

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
        <a href="#/transactions">交易</a>
      </nav>
      <hr />
      <PageComponent />
    </div>
  );
}

export default App;
