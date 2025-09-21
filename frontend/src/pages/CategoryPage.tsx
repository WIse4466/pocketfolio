import { useState, FormEvent } from 'react';

const API_URL = 'http://localhost:8080/api/categories';

export function CategoryPage() {
    const [name, setName] = useState('');

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();

        try {
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    name,
                    parentId: null, // For now, only creating top-level categories
                }),
            });

            if (!response.ok) {
                throw new Error('Failed to create category');
            }

            const createdCategory = await response.json();
            console.log('Category created:', createdCategory);
            alert(`分類 "${createdCategory.name}" 已成功建立！`);
            setName(''); // Clear form
        } catch (error) {
            console.error(error);
            alert('建立分類時發生錯誤。');
        }
    };

    return (
        <div>
            <h1>分類管理</h1>
            <form onSubmit={handleSubmit}>
                <h2>建立新分類</h2>
                <div>
                    <label htmlFor="name">名稱：</label>
                    <input
                        id="name"
                        type="text"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        required
                    />
                </div>
                <button type="submit">建立</button>
            </form>
        </div>
    );
}