import { useState, useEffect } from 'react';
import type { FormEvent, ChangeEvent } from 'react';

const API_URL = 'http://localhost:8080/api/accounts';

// Account type as string union to satisfy `erasableSyntaxOnly`
const ACCOUNT_TYPES = ['CASH','BANK','CREDIT_CARD','E_WALLET','INVESTMENT'] as const;
type AccountType = typeof ACCOUNT_TYPES[number];

// Interface for Account entity
interface Account {
    id: string;
    userId: string; // Assuming userId is a string UUID
    name: string;
    type: AccountType;
    currencyCode: string;
    initialBalance: number;
    currentBalance: number;
    includeInNetWorth: boolean;
    archived: boolean;
    closingDay?: number; // Optional
    dueDay?: number;     // Optional
    autopayAccount?: Account; // Self-referencing, optional
    notes?: string;
    createdAt: string;
    updatedAt: string;
}

export function AccountPage() {
    const [accounts, setAccounts] = useState<Account[]>([]);
    const [formData, setFormData] = useState({
        name: '',
        type: 'CASH' as AccountType, // Default to CASH
        currencyCode: 'TWD',
        initialBalance: 0,
        includeInNetWorth: true,
        archived: false,
        closingDay: undefined as number | undefined,
        dueDay: undefined as number | undefined,
        autopayAccountId: '' as string | undefined,
        notes: '',
    });

    const fetchAccounts = async () => {
        try {
            const response = await fetch(API_URL);
            if (!response.ok) {
                throw new Error('Failed to fetch accounts');
            }
            const data: Account[] = await response.json();
            setAccounts(data);
        } catch (error) {
            console.error('Error fetching accounts:', error);
            alert(`讀取帳戶列表時發生錯誤: ${error instanceof Error ? error.message : String(error)}`);
        }
    };

    useEffect(() => {
        fetchAccounts();
    }, []);

    const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
        const { name, value, type } = e.target;
        let parsedValue: string | number | boolean = value;

        if (type === 'number') {
            parsedValue = parseFloat(value);
            if (isNaN(parsedValue)) parsedValue = 0; // Handle empty or invalid number input
        } else if (type === 'checkbox') {
            parsedValue = (e.target as HTMLInputElement).checked;
        }

        // When switching away from CREDIT_CARD, clear CC-specific fields
        if (name === 'type') {
            const nextType = parsedValue as AccountType;
            setFormData(prev => ({
                ...prev,
                type: nextType,
                ...(nextType !== 'CREDIT_CARD' ? {
                    closingDay: undefined,
                    dueDay: undefined,
                    autopayAccountId: undefined,
                } : {})
            }));
            return;
        }

        setFormData(prev => ({
            ...prev,
            [name]: parsedValue
        }));
    };

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();

        // For now, hardcode userId. In a real app, this would come from auth context.
        const userId = '00000000-0000-0000-0000-000000000001'; 

        const accountToCreate = {
            userId,
            name: formData.name,
            type: formData.type,
            currencyCode: formData.currencyCode,
            initialBalance: formData.initialBalance,
            currentBalance: formData.initialBalance, // Current balance starts as initial balance
            includeInNetWorth: formData.includeInNetWorth,
            archived: formData.archived,
            closingDay: formData.closingDay || null, // Send null if undefined
            dueDay: formData.dueDay || null,         // Send null if undefined
            autopayAccount: formData.autopayAccountId ? { id: formData.autopayAccountId } : null, // Only send ID
            notes: formData.notes || null,
        };

        try {
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(accountToCreate),
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`後端錯誤: ${errorText}`);
            }

            alert(`帳戶 "${formData.name}" 已成功建立！`);
            setFormData({
                name: '',
                type: 'CASH',
                currencyCode: 'TWD',
                initialBalance: 0,
                includeInNetWorth: true,
                archived: false,
                closingDay: undefined,
                dueDay: undefined,
                autopayAccountId: undefined,
                notes: '',
            });
            fetchAccounts(); // Refresh the list
        } catch (error) {
            console.error('Error creating account:', error);
            alert(`建立帳戶時發生錯誤:\n${error instanceof Error ? error.message : String(error)}`);
        }
    };

    const handleDelete = async (id: string) => {
        if (!window.confirm('確定要刪除這個帳戶嗎？\n注意：如果帳戶有相關交易，刪除可能會失敗。')) {
            return;
        }

        try {
            const response = await fetch(`${API_URL}/${id}`, {
                method: 'DELETE',
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`後端錯誤: ${errorText}`);
            }

            alert('帳戶已成功刪除！');
            fetchAccounts(); // Refresh the list
        } catch (error) {
            console.error('Error deleting account:', error);
            alert(`刪除帳戶時發生錯誤:\n${error instanceof Error ? error.message : String(error)}`);
        }
    };

    // Filter accounts for autopay dropdown (exclude credit cards and the account itself if editing)
    const autopayOptions = accounts.filter(acc => acc.type !== 'CREDIT_CARD');

    return (
        <div>
            <h1>帳戶管理</h1>
            <form onSubmit={handleSubmit}>
                <h2>建立新帳戶</h2>
                <div>
                    <label htmlFor="name">名稱：</label>
                    <input id="name" name="name" type="text" value={formData.name} onChange={handleChange} required />
                </div>
                <div>
                    <label htmlFor="type">類型：</label>
                    <select id="type" name="type" value={formData.type} onChange={handleChange} required>
                        {ACCOUNT_TYPES.map(type => (
                            <option key={type} value={type}>{type}</option>
                        ))}
                    </select>
                </div>
                <div>
                    <label htmlFor="currencyCode">幣別：</label>
                    <input id="currencyCode" name="currencyCode" type="text" value={formData.currencyCode} onChange={handleChange} required maxLength={3} />
                </div>
                <div>
                    <label htmlFor="initialBalance">初始餘額：</label>
                    <input id="initialBalance" name="initialBalance" type="number" value={formData.initialBalance} onChange={handleChange} required step="0.01" />
                </div>
                <div>
                    <label htmlFor="includeInNetWorth">計入淨資產：</label>
                    <input id="includeInNetWorth" name="includeInNetWorth" type="checkbox" checked={formData.includeInNetWorth} onChange={handleChange} />
                </div>
                <div>
                    <label htmlFor="archived">已封存：</label>
                    <input id="archived" name="archived" type="checkbox" checked={formData.archived} onChange={handleChange} />
                </div>
                {formData.type === 'CREDIT_CARD' && (
                    <>
                        <div>
                            <label htmlFor="closingDay">結帳日 (信用卡)：</label>
                            <input id="closingDay" name="closingDay" type="number" value={formData.closingDay || ''} onChange={handleChange} min="1" max="31" />
                        </div>
                        <div>
                            <label htmlFor="dueDay">繳款日 (信用卡)：</label>
                            <input id="dueDay" name="dueDay" type="number" value={formData.dueDay || ''} onChange={handleChange} min="1" max="31" />
                        </div>
                        <div>
                            <label htmlFor="autopayAccountId">自動繳款帳戶：</label>
                            <select id="autopayAccountId" name="autopayAccountId" value={formData.autopayAccountId || ''} onChange={handleChange}>
                                <option value="">-- 無 --</option>
                                {autopayOptions.map(acc => (
                                    <option key={acc.id} value={acc.id}>{acc.name} ({acc.type})</option>
                                ))}
                            </select>
                        </div>
                    </>
                )}
                <div>
                    <label htmlFor="notes">備註：</label>
                    <textarea id="notes" name="notes" value={formData.notes} onChange={handleChange} maxLength={500} />
                </div>
                <button type="submit">建立</button>
            </form>

            <hr />

            <h2>現有帳戶</h2>
            {accounts.length > 0 ? (
                <ul>
                    {accounts.map(account => (
                        <li key={account.id}>
                            {account.name} ({account.type}) - {account.currencyCode} {account.currentBalance.toFixed(2)}
                            {account.includeInNetWorth ? ' (計入淨資產)' : ' (不計入淨資產)'}
                            {account.archived && ' (已封存)'}
                            <button onClick={() => handleDelete(account.id)}>刪除</button>
                        </li>
                    ))}
                </ul>
            ) : (
                <p>目前沒有任何帳戶。</p>
            )}
        </div>
    );
}
