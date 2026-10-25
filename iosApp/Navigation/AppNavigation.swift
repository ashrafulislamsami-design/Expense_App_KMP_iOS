import SwiftUI

struct AppNavigation: View {
    @State private var selectedTab: TabItem = .home
    @State private var showingQuickAdd: Bool = false
    
    enum TabItem: Int, CaseIterable {
        case home = 0
        case activity = 1
        case budgets = 2
        case goals = 3
        case settings = 4
    }
    
    var body: some View {
        TabView(selection: $selectedTab) {
            HomeDashboardView()
                .tabItem {
                    Label("Dashboard", systemImage: "chart.pie.fill")
                }
                .tag(TabItem.home)
            
            TransactionsListView()
                .tabItem {
                    Label("Activity", systemImage: "list.bullet.rectangle.portrait.fill")
                }
                .tag(TabItem.activity)
            
            BudgetsOverviewView()
                .tabItem {
                    Label("Budgets", systemImage: "chart.bar.xaxis")
                }
                .tag(TabItem.budgets)
            
            GoalsListView()
                .tabItem {
                    Label("Goals", systemImage: "leaf.fill")
                }
                .tag(TabItem.goals)
            
            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gearshape.fill")
                }
                .tag(TabItem.settings)
        }
        .tint(AppColors.brandPrimary)
    }
}
