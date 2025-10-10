import { useState, useEffect } from 'react';
import { apiUrl } from '../lib/api';
import type { FormEvent } from 'react';

const API_URL = apiUrl('/api/categories');

// Updated interface to support tree structure
interface Category {
    id: string;
    name: string;
    children?: Category[];
}

// Helper function to flatten the category tree for the dropdown
const flattenCategories = (categories: Category[]): { id: string; name: string; level: number }[] => {
    const allCategories: { id: string; name: string; level: number }[] = [];
    const recurse = (cats: Category[], level: number) => {
        for (const cat of cats) {
            allCategories.push({ id: cat.id, name: cat.name, level });
            if (cat.children) {
                recurse(cat.children, level + 1);
            }
        }
    };
    recurse(categories, 0);
    return allCategories;
};

// Recursive component to render the category list
const CategoryList = ({ categories, onDelete }: { categories: Category[], onDelete: (id: string) => void }) => {
    return (
        <ul>
            {categories.map((category) => (
                <li key={category.id}>
                    {category.name}{' '}
                    <button onClick={() => onDelete(category.id)}>刪除</button>
                    {category.children && category.children.length > 0 && (
                        <CategoryList categories={category.children} onDelete={onDelete} />
                    )}
                </li>
            ))}
        </ul>
    );
};

export function CategoryPage() {
    const [name, setName] = useState('');
    const [parentId, setParentId] = useState<string | null>(null);
    const [categories, setCategories] = useState<Category[]>([]);

    const fetchCategories = async () => {
        try {
            const response = await fetch(API_URL);
            if (!response.ok) {
                throw new Error('Failed to fetch categories');
            }
            const data: Category[] = await response.json();
            setCategories(data);
        } catch (error) {
            console.error(error);
            alert('讀取分類列表時發生錯誤。');
        }
    };

    useEffect(() => {
        fetchCategories();
    }, []);

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
                    parentId: parentId === '' ? null : parentId,
                }),
            });

            if (!response.ok) {
                const errorText = await response.text(); // Get detailed error text
                throw new Error(`後端錯誤: ${errorText}`);
            }

            const createdCategory = await response.json();
            alert(`分類 "${createdCategory.name}" 已成功建立！`);
            setName('');
            setParentId(null);
            fetchCategories();
        } catch (error) {
            console.error("建立分類時發生錯誤:", error);
            const message = error instanceof Error ? error.message : String(error);
            alert(`建立分類時發生錯誤:\n${message}`);
        }
    };

    const handleDelete = async (id: string) => {
        if (!window.confirm('確定要刪除這個分類嗎？\n注意：如果該分類底下有子分類，刪除將會失敗。')) {
            return;
        }

        try {
            const response = await fetch(`${API_URL}/${id}`, {
                method: 'DELETE',
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to delete category: ${errorText}`);
            }

            alert('分類已成功刪除！');
            fetchCategories();
        } catch (error) {
            console.error(error);
            alert(`刪除分類時發生錯誤: ${error}`);
        }
    };

    const flatCategories = flattenCategories(categories);

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
                <div>
                    <label htmlFor="parentId">父類別：</label>
                    <select
                        id="parentId"
                        value={parentId || ''}
                        onChange={(e) => setParentId(e.target.value || null)}
                    >
                        <option value="">-- 無 (設為頂層分類) --</option>
                        {flatCategories.map(cat => (
                            <option key={cat.id} value={cat.id}>
                                {'--'.repeat(cat.level)} {cat.name}
                            </option>
                        ))}
                    </select>
                </div>
                <button type="submit">建立</button>
            </form>

            <hr />

            <h2>現有分類</h2>
            {categories.length > 0 ? (
                <CategoryList categories={categories} onDelete={handleDelete} />
            ) : (
                <p>目前沒有任何分類。</p>
            )}
        </div>
    );
}
