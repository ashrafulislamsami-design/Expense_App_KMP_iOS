import Foundation
import UserNotifications

/**
 * 100% Offline Local Notification Scheduler for iOS.
 * Uses UNUserNotificationCenter to schedule daily evening reminders without network connectivity.
 */
class NotificationScheduler {
    static let shared = NotificationScheduler()
    
    private init() {}
    
    func requestPermission(completion: @escaping (Bool) -> Void) {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            DispatchQueue.main.async {
                completion(granted)
            }
        }
    }
    
    func scheduleDailyReminder(hour: Int = 21, minute: Int = 0) {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ["daily_expense_reminder"])
        
        let content = UNMutableNotificationContent()
        content.title = "Record Today's Spending"
        content.body = "Take 30 seconds to log today's expenses and keep your daily safe-to-spend pace on track."
        content.sound = .default
        
        var dateComponents = DateComponents()
        dateComponents.hour = hour
        dateComponents.minute = minute
        
        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
        let request = UNNotificationRequest(identifier: "daily_expense_reminder", content: content, trigger: trigger)
        
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("Failed to schedule daily reminder: \(error.localizedDescription)")
            }
        }
    }
}
