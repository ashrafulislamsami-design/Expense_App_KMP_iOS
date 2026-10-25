// preview/app.js - Executive Reactive Financial Engine & State Management
(function () {
    'use strict';

    // Strict integer minor units math (1 Taka = 100 Poisha)
    const MinorUnits = {
        fromDecimal: (val) => Math.round((parseFloat(val) || 0) * 100),
        format: (minorUnits, symbol = "৳") => {
            const isNegative = minorUnits < 0;
            const absVal = Math.abs(minorUnits);
            const taka = Math.floor(absVal / 100);
            const poisha = absVal % 100;
            const poishaStr = poisha.toString().padStart(2, '0');
            const takaFormatted = taka.toLocaleString('en-US');
            return `${isNegative ? '-' : ''}${symbol}${takaFormatted}.${poishaStr}`;
        }
    };

    // Salary Cycle Calculator
    function calculateSalaryCycle(startDay = 1) {
        const now = new Date();
        const year = now.getFullYear();
        const month = now.getMonth();
        const todayDate = now.getDate();

        let startDate, endDate;
        if (todayDate >= startDay) {
            startDate = new Date(year, month, startDay);
            const nextMonthDays = new Date(year, month + 2, 0).getDate();
            const endDayClamped = Math.min(startDay - 1 === 0 ? new Date(year, month + 1, 0).getDate() : startDay - 1, nextMonthDays);
            endDate = new Date(year, month + 1, endDayClamped);
        } else {
            const prevMonthDays = new Date(year, month, 0).getDate();
            startDate = new Date(year, month - 1, Math.min(startDay, prevMonthDays));
            endDate = new Date(year, month, startDay - 1);
        }

        const oneDayMs = 24 * 60 * 60 * 1000;
        const totalDays = Math.max(1, Math.round((endDate - startDate) / oneDayMs) + 1);
        const daysRemaining = Math.max(1, Math.round((endDate - now) / oneDayMs) + 1);

        const formatDateStr = (d) => d.toISOString().split('T')[0];

        return {
            startDateStr: formatDateStr(startDate),
            endDateStr: formatDateStr(endDate),
            totalDays,
            daysRemaining
        };
    }

    const STORAGE_KEY = 'expense_app_executive_preview_v2';

    const defaultCategories = [
        { id: 1, name: 'Food', icon: '🍽️', color: '#F97316' },
        { id: 2, name: 'Transport', icon: '🚗', color: '#3B82F6' },
        { id: 3, name: 'Groceries', icon: '🛒', color: '#10B981' },
        { id: 4, name: 'Shopping', icon: '🛍️', color: '#8B5CF6' },
        { id: 5, name: 'Bills', icon: '⚡', color: '#F59E0B' },
        { id: 6, name: 'Entertainment', icon: '🎬', color: '#EC4899' },
        { id: 7, name: 'Salary', icon: '💼', color: '#10B981' }
    ];

    const defaultAccounts = [
        { id: 1, name: 'Cash', balance: 500000 },
        { id: 2, name: 'Prime Bank', balance: 11500000 },
        { id: 3, name: 'bKash', balance: 250000 },
        { id: 4, name: 'Nagad', balance: 150000 }
    ];

    const initialTransactions = [
        {
            id: 1,
            amount: 8000, // ৳80.00
            type: 'EXPENSE',
            categoryId: 1,
            categoryName: 'Food',
            categoryIcon: '🍽️',
            accountId: 1,
            accountName: 'Cash',
            note: 'Breakfast & Tea',
            date: new Date().toISOString().split('T')[0],
            time: '08:30'
        },
        {
            id: 2,
            amount: 45000, // ৳450.00
            type: 'EXPENSE',
            categoryId: 2,
            categoryName: 'Transport',
            categoryIcon: '🚗',
            accountId: 3,
            accountName: 'bKash',
            note: 'Ride to Office',
            date: new Date().toISOString().split('T')[0],
            time: '09:15'
        },
        {
            id: 3,
            amount: 120000, // ৳1,200.00
            type: 'EXPENSE',
            categoryId: 3,
            categoryName: 'Groceries',
            categoryIcon: '🛒',
            accountId: 2,
            accountName: 'Prime Bank',
            note: 'Fresh Market Vegetables',
            date: new Date().toISOString().split('T')[0],
            time: '12:45'
        }
    ];

    function loadState() {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) {
            try {
                return JSON.parse(stored);
            } catch (e) {
                console.error('Failed to parse state:', e);
            }
        }
        return {
            config: {
                salaryAmount: 12000000, // ৳120,000.00 in poisha
                cycleStartDay: 1,
                currencySymbol: '৳'
            },
            transactions: initialTransactions,
            categories: defaultCategories,
            accounts: defaultAccounts,
            currentTab: 'dashboard',
            theme: 'dark',
            borderless: false,
            searchQuery: '',
            filterType: null
        };
    }

    let state = loadState();

    function saveState() {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    }

    function computeFinancials() {
        const cycle = calculateSalaryCycle(state.config.cycleStartDay);
        let totalExpense = 0;
        let totalIncome = 0;
        let totalTransfers = 0;

        state.transactions.forEach(tx => {
            if (tx.type === 'EXPENSE') {
                totalExpense += tx.amount;
            } else if (tx.type === 'INCOME') {
                totalIncome += tx.amount;
            } else if (tx.type === 'TRANSFER') {
                totalTransfers += tx.amount;
            }
        });

        const effectiveIncome = state.config.salaryAmount + totalIncome;
        const remainingBudget = Math.max(0, effectiveIncome - totalExpense);
        const safePerDay = Math.floor(remainingBudget / cycle.daysRemaining);

        const budgetLimit = state.config.salaryAmount;
        const progress = budgetLimit > 0 ? Math.min(100, Math.round((totalExpense / budgetLimit) * 100)) : 0;

        return {
            cycle,
            totalExpense,
            totalIncome,
            totalTransfers,
            effectiveIncome,
            remainingBudget,
            safePerDay,
            progress
        };
    }

    function renderApp() {
        document.documentElement.setAttribute('data-theme', state.theme);
        const chassis = document.getElementById('deviceChassis');
        if (chassis) {
            if (state.borderless) chassis.classList.add('borderless');
            else chassis.classList.remove('borderless');
        }

        const clockEl = document.getElementById('statusClock');
        if (clockEl) {
            const now = new Date();
            clockEl.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
        }

        const fin = computeFinancials();

        const badgePill = document.getElementById('cycleRemainingPill');
        if (badgePill) {
            badgePill.textContent = `${fin.cycle.daysRemaining}d left`;
        }

        const contentContainer = document.getElementById('screenContent');
        if (!contentContainer) return;

        if (state.currentTab === 'dashboard') {
            renderDashboard(contentContainer, fin);
        } else if (state.currentTab === 'transactions') {
            renderTransactions(contentContainer, fin);
        } else if (state.currentTab === 'budgets') {
            renderBudgets(contentContainer, fin);
        } else if (state.currentTab === 'settings') {
            renderSettings(contentContainer, fin);
        }

        document.querySelectorAll('.nav-tab').forEach(tab => {
            const tabName = tab.getAttribute('data-tab');
            if (tabName === state.currentTab) {
                tab.classList.add('active');
            } else {
                tab.classList.remove('active');
            }
        });

        updateSidebarStats(fin);
    }

    function renderDashboard(container, fin) {
        const symbol = state.config.currencySymbol;
        const recentTxs = state.transactions.slice(0, 5);

        // Compute top spending categories
        const expenses = state.transactions.filter(t => t.type === 'EXPENSE');
        const catMap = {};
        expenses.forEach(t => {
            const name = t.categoryName || 'Other';
            catMap[name] = (catMap[name] || 0) + t.amount;
        });
        const totalExp = fin.totalExpense || 1;
        const topCategories = Object.keys(catMap).map(name => ({
            name,
            amount: catMap[name],
            percentage: Math.min(100, Math.max(1, Math.round((catMap[name] / totalExp) * 100)))
        })).sort((a, b) => b.amount - a.amount).slice(0, 4);

        let categoryListHtml = '';
        if (topCategories.length === 0) {
            categoryListHtml = `
                <div style="text-align: center; padding: 14px; font-size: 13px; color: var(--color-on-surface-variant);">
                    No category spending recorded this cycle yet.
                </div>
            `;
        } else {
            categoryListHtml = topCategories.map((c, i) => `
                <div style="margin-bottom: ${i < topCategories.length - 1 ? '12px' : '0'};">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                        <div style="font-size: 13px; font-weight: 500; color: var(--color-on-surface);">
                            ${escapeHtml(c.name)} <span style="font-size: 11px; color: var(--color-on-surface-variant); margin-left: 4px;">${c.percentage}%</span>
                        </div>
                        <div style="font-size: 13px; font-weight: 600; color: var(--color-on-surface); font-variant-numeric: tabular-nums;">
                            ${MinorUnits.format(c.amount, symbol)}
                        </div>
                    </div>
                    <div style="height: 4px; background: var(--color-surface-variant); border-radius: 4px; overflow: hidden;">
                        <div style="width: ${c.percentage}%; height: 100%; background: var(--emerald-primary); border-radius: 4px;"></div>
                    </div>
                </div>
            `).join('');
        }

        let txListHtml = '';
        if (recentTxs.length === 0) {
            txListHtml = `
                <div class="empty-state" style="padding: 20px 16px;">
                    <div class="empty-state-title" style="font-size: 14px; font-weight: 600;">No transactions yet</div>
                    <div class="empty-state-msg" style="font-size: 12px; margin-top: 4px;">Add your first transaction to start tracking where your money goes.</div>
                    <button class="studio-btn primary" id="quickAddEmptyBtn" style="margin-top: 12px; font-size: 12px;">+ Add Transaction</button>
                </div>
            `;
        } else {
            txListHtml = recentTxs.map((tx, idx) => `
                <div class="tx-item" data-id="${tx.id}" style="padding: 10px 0; ${idx < recentTxs.length - 1 ? 'border-bottom: 1px solid rgba(255,255,255,0.06);' : ''}">
                    <div class="tx-left">
                        <div class="tx-avatar" style="background: rgba(16, 185, 129, 0.12); width: 38px; height: 38px; border-radius: 8px;">
                            ${tx.categoryIcon || '🧾'}
                        </div>
                        <div class="tx-info">
                            <span class="tx-note" style="font-weight: 500;">${escapeHtml(tx.note || tx.categoryName || 'Transaction')}</span>
                            <span class="tx-meta" style="font-size: 11px;">${tx.date} &bull; ${tx.type}</span>
                        </div>
                    </div>
                    <div style="display:flex; align-items:center; gap: 8px;">
                        <span class="tx-amount ${tx.type.toLowerCase()}" style="font-weight: 600; font-variant-numeric: tabular-nums;">
                            ${tx.type === 'INCOME' ? '+' : '-'}${MinorUnits.format(tx.amount, symbol)}
                        </span>
                        <button class="delete-tx-btn" data-id="${tx.id}" style="padding: 2px 6px; font-size: 11px; background: rgba(244,63,94,0.1); color: var(--color-expense); border:none; border-radius:6px; cursor:pointer;" title="Delete">✕</button>
                    </div>
                </div>
            `).join('');
        }

        container.innerHTML = `
            <!-- Section A: Primary Balance -->
            <div style="padding: 4px 0 8px 0;">
                <div style="font-size: 11px; font-weight: 600; letter-spacing: 0.8px; color: var(--color-on-surface-variant); text-transform: uppercase;">
                    AVAILABLE BALANCE
                </div>
                <div style="font-size: 28px; font-weight: 700; color: var(--emerald-primary); margin: 4px 0; letter-spacing: -0.5px; font-variant-numeric: tabular-nums;">
                    ${MinorUnits.format(fin.remainingBudget, symbol)}
                </div>
                <div style="font-size: 12px; color: var(--color-on-surface-variant);">
                    Salary cycle &bull; ${fin.cycle.daysRemaining} days remaining
                </div>
            </div>

            <!-- Section B: Safe to Spend Today -->
            <div style="background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 16px; padding: 16px;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                    <div style="display: flex; align-items: center; gap: 6px; font-size: 11px; font-weight: 600; letter-spacing: 0.5px; color: var(--emerald-primary);">
                        <span>📈</span>
                        <span>SAFE TO SPEND TODAY</span>
                    </div>
                    <span style="font-size: 11px; color: var(--color-on-surface-variant);">${fin.cycle.daysRemaining} days left</span>
                </div>
                <div style="font-size: 22px; font-weight: 700; color: var(--color-on-surface); margin-bottom: 4px; font-variant-numeric: tabular-nums;">
                    ${MinorUnits.format(fin.safePerDay, symbol)}
                </div>
                <div style="font-size: 12px; color: var(--color-on-surface-variant); line-height: 1.4;">
                    ${MinorUnits.format(fin.safePerDay, symbol)}/day left across ${fin.cycle.daysRemaining} remaining days in this salary cycle.
                </div>
            </div>

            <!-- Section C: Monthly Overview (3-Column Cohesive Card) -->
            <div style="background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 12px; padding: 16px;">
                <div style="font-size: 11px; font-weight: 600; letter-spacing: 0.5px; color: var(--color-on-surface-variant); text-transform: uppercase; margin-bottom: 12px;">
                    THIS MONTH
                </div>
                <div style="display: flex; justify-content: space-between; gap: 8px;">
                    <div>
                        <div style="font-size: 11px; color: var(--color-on-surface-variant); margin-bottom: 2px;">Income</div>
                        <div style="font-size: 15px; font-weight: 700; color: var(--color-income); font-variant-numeric: tabular-nums;">
                            ${MinorUnits.format(state.config.salaryAmount, symbol)}
                        </div>
                    </div>
                    <div>
                        <div style="font-size: 11px; color: var(--color-on-surface-variant); margin-bottom: 2px;">Spent</div>
                        <div style="font-size: 15px; font-weight: 700; color: var(--color-expense); font-variant-numeric: tabular-nums;">
                            ${MinorUnits.format(fin.totalExpense, symbol)}
                        </div>
                    </div>
                    <div>
                        <div style="font-size: 11px; color: var(--color-on-surface-variant); margin-bottom: 2px;">Remaining</div>
                        <div style="font-size: 15px; font-weight: 700; color: var(--emerald-primary); font-variant-numeric: tabular-nums;">
                            ${MinorUnits.format(fin.remainingBudget, symbol)}
                        </div>
                    </div>
                </div>
                <div style="margin-top: 14px; height: 4px; background: var(--color-surface-variant); border-radius: 4px; overflow: hidden;">
                    <div style="width: ${fin.progress}%; height: 100%; background: ${fin.progress > 90 ? 'var(--color-expense)' : 'var(--emerald-primary)'}; border-radius: 4px;"></div>
                </div>
            </div>

            <!-- Section D: Where Your Money Went -->
            <div>
                <div style="font-size: 11px; font-weight: 600; letter-spacing: 0.5px; color: var(--color-on-surface-variant); text-transform: uppercase; margin-bottom: 8px;">
                    WHERE YOUR MONEY WENT
                </div>
                <div style="background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 12px; padding: 14px;">
                    ${categoryListHtml}
                </div>
            </div>

            <!-- Section E: Recent Activity -->
            <div>
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                    <span style="font-size: 15px; font-weight: 600; color: var(--color-on-surface);">Recent Activity</span>
                    ${recentTxs.length > 0 ? '<button class="btn-view-all" id="btnViewAll" style="font-size: 12px; font-weight: 600; color: var(--emerald-primary); background: none; border: none; cursor: pointer;">View all</button>' : ''}
                </div>
                <div style="background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 12px; padding: 4px 14px;">
                    ${txListHtml}
                </div>
            </div>
        `;

        document.getElementById('btnViewAll')?.addEventListener('click', () => {
            state.currentTab = 'transactions';
            renderApp();
        });

        document.getElementById('quickAddEmptyBtn')?.addEventListener('click', () => openQuickAddModal('EXPENSE'));

        attachTxDeleteListeners();
    }

    function renderTransactions(container, fin) {
        const symbol = state.config.currencySymbol;
        const query = (state.searchQuery || '').toLowerCase();
        const filtered = state.transactions.filter(tx => {
            if (state.filterType && tx.type !== state.filterType) return false;
            if (!query) return true;
            return (tx.note && tx.note.toLowerCase().includes(query)) ||
                   (tx.categoryName && tx.categoryName.toLowerCase().includes(query)) ||
                   (tx.type && tx.type.toLowerCase().includes(query));
        });

        // Group by date
        const groups = {};
        filtered.forEach(tx => {
            const d = tx.date || 'Today';
            if (!groups[d]) groups[d] = [];
            groups[d].push(tx);
        });

        const sortedDates = Object.keys(groups).sort().reverse();

        let listHtml = '';
        if (sortedDates.length === 0) {
            listHtml = `
                <div class="empty-state" style="padding: 32px 16px;">
                    <div class="empty-state-title" style="font-size: 14px; font-weight: 600;">No transactions found</div>
                    <div class="empty-state-msg" style="font-size: 12px; margin-top: 4px;">No records match your active search query or filter.</div>
                    <button class="studio-btn" id="clearFiltersBtn" style="margin-top: 12px; font-size: 12px;">Clear Filters</button>
                </div>
            `;
        } else {
            listHtml = sortedDates.map(dateStr => {
                const dayTxs = groups[dateStr];
                const dayExpense = dayTxs.filter(t => t.type === 'EXPENSE' || t.type === 'DONATION' || t.type === 'DEBT_PAYMENT').reduce((acc, t) => acc + t.amount, 0);
                const dayRefund = dayTxs.filter(t => t.type === 'REFUND').reduce((acc, t) => acc + t.amount, 0);
                const netDayExpense = Math.max(0, dayExpense - dayRefund);

                return `
                    <div style="margin-bottom: 14px;">
                        <div style="display: flex; justify-content: space-between; align-items: center; padding: 4px 6px; margin-bottom: 4px;">
                            <span style="font-size: 12px; font-weight: 600; color: var(--color-on-surface-variant);">${dateStr}</span>
                            ${netDayExpense > 0 ? `<span style="font-size: 11px; font-weight: 600; color: var(--color-on-surface-variant); font-variant-numeric: tabular-nums;">−${MinorUnits.format(netDayExpense, symbol)}</span>` : ''}
                        </div>
                        <div style="background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 12px; padding: 4px 14px;">
                            ${dayTxs.map((tx, idx) => {
                                let sign = '−';
                                let amtColor = 'var(--color-expense)';
                                let isRefund = tx.type === 'REFUND';
                                let accountRoute = tx.accountName || 'Cash';

                                if (tx.type === 'INCOME') {
                                    sign = '+';
                                    amtColor = 'var(--color-income)';
                                } else if (tx.type === 'REFUND') {
                                    sign = '+';
                                    amtColor = '#06B6D4';
                                } else if (tx.type === 'TRANSFER') {
                                    sign = '';
                                    amtColor = 'var(--color-on-surface)';
                                    accountRoute = `${tx.accountName || 'Cash'} → ${tx.destinationAccountName || 'bKash'}`;
                                } else if (tx.type === 'SAVING') {
                                    sign = '';
                                    amtColor = '#0EA5E9';
                                } else if (tx.type === 'DONATION') {
                                    sign = '−';
                                    amtColor = '#EC4899';
                                }

                                return `
                                <div class="tx-item" data-id="${tx.id}" style="padding: 10px 0; ${idx < dayTxs.length - 1 ? 'border-bottom: 1px solid rgba(255,255,255,0.06);' : ''}">
                                    <div class="tx-left">
                                        <div class="tx-avatar" style="background: rgba(16, 185, 129, 0.12); width: 38px; height: 38px; border-radius: 8px;">
                                            ${tx.categoryIcon || '🧾'}
                                        </div>
                                        <div class="tx-info">
                                            <div style="display:flex; align-items:center; gap:6px;">
                                                <span class="tx-note" style="font-weight: 600;">${escapeHtml(tx.note || tx.categoryName || 'Transaction')}</span>
                                                ${isRefund ? '<span style="font-size:9px; font-weight:700; background:rgba(6,182,212,0.18); color:#06B6D4; padding:1px 4px; border-radius:4px;">REFUND</span>' : ''}
                                            </div>
                                            <span class="tx-meta" style="font-size: 11px; color: var(--color-on-surface-variant);">${tx.categoryName || 'General'} &bull; ${accountRoute} &bull; ${tx.time || '12:00'}</span>
                                        </div>
                                    </div>
                                    <div style="display:flex; align-items:center; gap: 8px;">
                                        <span style="font-weight: 700; font-variant-numeric: tabular-nums; color: ${amtColor};">
                                            ${sign}${MinorUnits.format(tx.amount, symbol)}
                                        </span>
                                        <button class="delete-tx-btn" data-id="${tx.id}" style="padding: 2px 6px; font-size: 11px; background: rgba(244,63,94,0.1); color: var(--color-expense); border:none; border-radius:6px; cursor:pointer;" title="Delete">✕</button>
                                    </div>
                                </div>
                            `}).join('')}
                        </div>
                    </div>
                `;
            }).join('');
        }

        container.innerHTML = `
            <div style="display:flex; flex-direction:column; gap:10px; margin-bottom: 12px;">
                <input type="text" id="txSearchInput" class="text-input-field" placeholder="🔍 Search note, category..." value="${escapeHtml(state.searchQuery || '')}" />
                
                <div style="display:flex; gap:8px; overflow-x:auto; padding-bottom: 2px;">
                    <button class="studio-btn ${state.filterType === null ? 'primary' : ''}" style="padding:5px 12px; font-size:11px; border-radius: 20px;" id="filterAllBtn">All</button>
                    <button class="studio-btn ${state.filterType === 'EXPENSE' ? 'primary' : ''}" style="padding:5px 12px; font-size:11px; border-radius: 20px;" id="filterExpBtn">Expenses</button>
                    <button class="studio-btn ${state.filterType === 'INCOME' ? 'primary' : ''}" style="padding:5px 12px; font-size:11px; border-radius: 20px;" id="filterIncBtn">Income</button>
                    <button class="studio-btn ${state.filterType === 'TRANSFER' ? 'primary' : ''}" style="padding:5px 12px; font-size:11px; border-radius: 20px;" id="filterTrfBtn">Transfers</button>
                </div>
            </div>

            ${listHtml}
        `;

        const searchInput = document.getElementById('txSearchInput');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                state.searchQuery = e.target.value;
                renderApp();
            });
        }

        document.getElementById('filterAllBtn')?.addEventListener('click', () => { state.filterType = null; renderApp(); });
        document.getElementById('filterExpBtn')?.addEventListener('click', () => { state.filterType = 'EXPENSE'; renderApp(); });
        document.getElementById('filterIncBtn')?.addEventListener('click', () => { state.filterType = 'INCOME'; renderApp(); });
        document.getElementById('filterTrfBtn')?.addEventListener('click', () => { state.filterType = 'TRANSFER'; renderApp(); });
        document.getElementById('clearFiltersBtn')?.addEventListener('click', () => { state.filterType = null; state.searchQuery = ''; renderApp(); });

        attachTxDeleteListeners();
    }

    function renderBudgets(container, fin) {
        const symbol = state.config.currencySymbol;
        container.innerHTML = `
            <div class="section-header">
                <span class="section-title">Salary Cycle Budgets</span>
            </div>
            <div class="budget-card">
                <div class="budget-header">
                    <div class="budget-header-left">
                        <span class="budget-title">Monthly Limit</span>
                        <span class="budget-details">${MinorUnits.format(fin.totalExpense, symbol)} / ${MinorUnits.format(state.config.salaryAmount, symbol)}</span>
                    </div>
                    <div class="budget-pill" style="background: rgba(16, 185, 129, 0.15); color: var(--emerald-light);">
                        ${fin.progress}%
                    </div>
                </div>
                <div class="progress-bar-bg">
                    <div class="progress-bar-fill" style="width: ${fin.progress}%; background: var(--emerald-primary);"></div>
                </div>
            </div>

            <div class="section-header" style="margin-top:8px;">
                <span class="section-title">Category Spending Limits</span>
            </div>
            <div style="display:flex; flex-direction:column; gap:10px;">
                ${state.categories.filter(c => c.name !== 'Salary').map(cat => {
                    const catTxs = state.transactions.filter(t => t.categoryName === cat.name && t.type === 'EXPENSE');
                    const spent = catTxs.reduce((acc, t) => acc + t.amount, 0);
                    const mockLimit = 2500000;
                    const catProg = Math.min(100, Math.round((spent / mockLimit) * 100));
                    return `
                        <div class="metric-card">
                            <div class="metric-header">
                                <span class="metric-title">${cat.icon} ${cat.name}</span>
                                <span style="font-size:11px; font-weight:700; color: ${catProg > 80 ? 'var(--color-expense)' : 'var(--emerald-light)'}">${catProg}%</span>
                            </div>
                            <div class="progress-bar-bg" style="margin: 4px 0;">
                                <div class="progress-bar-fill" style="width: ${catProg}%; background: ${catProg > 80 ? 'var(--color-expense)' : 'var(--emerald-primary)'};"></div>
                            </div>
                            <div style="display:flex; justify-content:space-between; font-size:11px; color:var(--text-secondary);">
                                <span>Spent: ${MinorUnits.format(spent, symbol)}</span>
                                <span>Limit: ${MinorUnits.format(mockLimit, symbol)}</span>
                            </div>
                        </div>
                    `;
                }).join('')}
            </div>
        `;
    }

    function renderSettings(container, fin) {
        const symbol = state.config.currencySymbol;
        container.innerHTML = `
            <div class="section-header">
                <span class="section-title">Preferences & Salary Configuration</span>
            </div>
            <div style="display:flex; flex-direction:column; gap:12px;">
                <div class="metric-card">
                    <span class="metric-title">Monthly Net Salary (৳)</span>
                    <input type="number" id="settingSalaryInput" class="text-input-field" style="margin-top:6px;" value="${state.config.salaryAmount / 100}" />
                </div>
                <div class="metric-card">
                    <span class="metric-title">Cycle Payday (Day of Month)</span>
                    <input type="number" id="settingStartDayInput" min="1" max="28" class="text-input-field" style="margin-top:6px;" value="${state.config.cycleStartDay}" />
                </div>
                <div class="metric-card">
                    <span class="metric-title">Currency Symbol</span>
                    <input type="text" id="settingCurrencyInput" class="text-input-field" style="margin-top:6px;" value="${state.config.currencySymbol}" />
                </div>
                <button class="studio-btn primary" id="btnSaveSettings" style="margin-top:6px; padding:12px;">Save Settings</button>
            </div>
        `;

        document.getElementById('btnSaveSettings')?.addEventListener('click', () => {
            const salInput = document.getElementById('settingSalaryInput');
            const startDayInput = document.getElementById('settingStartDayInput');
            const currInput = document.getElementById('settingCurrencyInput');

            if (salInput) state.config.salaryAmount = MinorUnits.fromDecimal(salInput.value);
            if (startDayInput) state.config.cycleStartDay = parseInt(startDayInput.value, 10) || 1;
            if (currInput) state.config.currencySymbol = currInput.value || '৳';

            saveState();
            renderApp();
            showToast("Settings Updated Successfully");
        });
    }

    function attachTxDeleteListeners() {
        document.querySelectorAll('.delete-tx-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const id = parseInt(btn.getAttribute('data-id'), 10);
                state.transactions = state.transactions.filter(t => t.id !== id);
                saveState();
                renderApp();
                showToast("Transaction Deleted");
            });
        });
    }

    // Modal Interaction
    let currentModalType = 'EXPENSE';
    let currentModalCategory = 'Food';
    let currentModalAccount = 'Cash';

    function openQuickAddModal(type = 'EXPENSE') {
        const modal = document.getElementById('quickAddModal');
        if (!modal) return;
        currentModalType = type;
        modal.classList.add('open');
        document.getElementById('modalAmountField').value = '80';
        document.getElementById('modalNoteField').value = '';
        updateModalTypeButtons();
        renderModalCategories();
        renderModalAccounts();
        document.getElementById('modalAmountField').focus();
    }

    function closeQuickAddModal() {
        const modal = document.getElementById('quickAddModal');
        if (modal) modal.classList.remove('open');
    }

    function updateModalTypeButtons() {
        document.querySelectorAll('.type-segment-btn').forEach(btn => {
            const t = btn.getAttribute('data-type');
            if (t === currentModalType) {
                btn.className = `type-segment-btn active ${t.toLowerCase()}`;
            } else {
                btn.className = 'type-segment-btn';
            }
        });
    }

    function renderModalCategories() {
        const container = document.getElementById('modalCategoryChips');
        if (!container) return;
        container.innerHTML = state.categories.map(cat => `
            <div class="category-chip ${currentModalCategory === cat.name ? 'selected' : ''}" data-cat="${cat.name}">
                <span>${cat.icon}</span>
                <span>${cat.name}</span>
            </div>
        `).join('');

        container.querySelectorAll('.category-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                currentModalCategory = chip.getAttribute('data-cat');
                renderModalCategories();
            });
        });
    }

    function renderModalAccounts() {
        const container = document.getElementById('modalAccountChips');
        if (!container) return;
        container.innerHTML = state.accounts.map(acc => `
            <div class="category-chip ${currentModalAccount === acc.name ? 'selected' : ''}" data-acc="${acc.name}">
                <span>🏦</span>
                <span>${acc.name}</span>
            </div>
        `).join('');

        container.querySelectorAll('.category-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                currentModalAccount = chip.getAttribute('data-acc');
                renderModalAccounts();
            });
        });
    }

    function saveQuickAddTransaction() {
        const amtInput = document.getElementById('modalAmountField');
        const noteInput = document.getElementById('modalNoteField');
        const amountMinor = MinorUnits.fromDecimal(amtInput.value);

        if (amountMinor <= 0) {
            alert('Please enter a valid amount greater than 0');
            return;
        }

        const now = new Date();
        const catObj = state.categories.find(c => c.name === currentModalCategory) || state.categories[0];
        const accObj = state.accounts.find(a => a.name === currentModalAccount) || state.accounts[0];

        const newTx = {
            id: Date.now(),
            amount: amountMinor,
            type: currentModalType,
            categoryId: catObj.id,
            categoryName: catObj.name,
            categoryIcon: catObj.icon,
            accountId: accObj.id,
            accountName: accObj.name,
            note: noteInput.value.trim() || catObj.name,
            date: now.toISOString().split('T')[0],
            time: now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })
        };

        state.transactions.unshift(newTx);
        saveState();
        closeQuickAddModal();
        renderApp();
        showToast(`Saved ${MinorUnits.format(amountMinor, state.config.currencySymbol)}`);
    }

    function showToast(msg) {
        const toast = document.getElementById('toastNotice');
        if (!toast) return;
        toast.textContent = msg;
        toast.classList.add('show');
        setTimeout(() => {
            toast.classList.remove('show');
        }, 2200);
    }

    function updateSidebarStats(fin) {
        const setVal = (id, text) => {
            const el = document.getElementById(id);
            if (el) el.textContent = text;
        };

        setVal('statTxCount', state.transactions.length.toString());
        setVal('statNetSpent', MinorUnits.format(fin.totalExpense, state.config.currencySymbol));
        setVal('statSafeToSpend', MinorUnits.format(fin.safePerDay, state.config.currencySymbol));
        setVal('statRemainingDays', `${fin.cycle.daysRemaining} days`);
    }

    function escapeHtml(str) {
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    function setupListeners() {
        document.querySelectorAll('.nav-tab').forEach(tab => {
            tab.addEventListener('click', () => {
                state.currentTab = tab.getAttribute('data-tab');
                renderApp();
            });
        });

        document.getElementById('fabAdd')?.addEventListener('click', () => openQuickAddModal('EXPENSE'));
        document.getElementById('modalCloseBtn')?.addEventListener('click', closeQuickAddModal);

        document.querySelectorAll('.type-segment-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                currentModalType = btn.getAttribute('data-type');
                updateModalTypeButtons();
            });
        });

        document.getElementById('modalSaveBtn')?.addEventListener('click', saveQuickAddTransaction);

        document.getElementById('btnToggleTheme')?.addEventListener('click', () => {
            state.theme = state.theme === 'dark' ? 'light' : 'dark';
            saveState();
            renderApp();
        });

        document.getElementById('btnToggleChassis')?.addEventListener('click', () => {
            state.borderless = !state.borderless;
            saveState();
            renderApp();
        });

        document.getElementById('btnInjectSample')?.addEventListener('click', () => {
            const samples = [
                { id: Date.now() + 1, amount: 8000, type: 'EXPENSE', categoryName: 'Food', categoryIcon: '🍽️', accountName: 'Cash', note: 'Tea & Snacks', date: new Date().toISOString().split('T')[0], time: '10:00' },
                { id: Date.now() + 2, amount: 25000, type: 'EXPENSE', categoryName: 'Transport', categoryIcon: '🚗', accountName: 'bKash', note: 'Rickshaw fare', date: new Date().toISOString().split('T')[0], time: '11:30' },
                { id: Date.now() + 3, amount: 150000, type: 'EXPENSE', categoryName: 'Shopping', categoryIcon: '🛍️', accountName: 'Prime Bank', note: 'Bookstore purchase', date: new Date().toISOString().split('T')[0], time: '15:15' }
            ];
            state.transactions.unshift(...samples);
            saveState();
            renderApp();
            showToast("Injected 3 Test Transactions");
        });

        document.getElementById('btnResetData')?.addEventListener('click', () => {
            if (confirm('Reset transactions to empty state?')) {
                state.transactions = [];
                saveState();
                renderApp();
                showToast("Ledger reset");
            }
        });
    }

    window.previewApp = {
        init: () => {
            setupListeners();
            renderApp();
            setInterval(() => {
                const clockEl = document.getElementById('statusClock');
                if (clockEl) {
                    const now = new Date();
                    clockEl.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
                }
            }, 1000);
        }
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', window.previewApp.init);
    } else {
        window.previewApp.init();
    }
})();
