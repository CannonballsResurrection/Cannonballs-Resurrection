import Foundation

/// The original bot chat tables and triggers, VERBATIM from the decompiled
/// Network.java:71-80 / postBotMessage + call sites in Cannon.java/Weapon.java.
enum BotChat {
    // Network.java string tables (counts as in source: ComplimentCount=5, so
    // "That was sweet" is unreachable in the original — kept for the record).
    static let insults = ["I'm gonna get you,", "You better watch out",
                          "Here comes the pain", "I got you in my sights", "You're going down"]
    static let greetings = ["Hi everybody", "Hello", "Hi guys!", "Greets", "Hola",
                            "Hey", "Hey folks", "What's up?", "Howdy"]
    static let deaths = ["Ow!", "Ouch!", "That hurt!", "Good grief!"]
    static let kills = ["Oh yeah!", "I am the king!", "Sweet!", "Take that!",
                        "Woohoo!", "I rule!", "Bow down before me!!"]
    static let compliments = ["Nice shot,", "That was a nice one,", "Good shot",
                              "Nice moves", "What a shot"]   // [5] "That was sweet" never picked

    private static func post(_ bot: Cannon, _ msg: String, game: GameController) {
        game.hud.botChat(bot.name, msg)
    }

    /// First ready turn: 25% greeting (Cannon.java:891-895).
    static func maybeGreet(_ bot: Cannon, game: GameController) {
        guard !bot.bot.hasGreeted else { return }
        bot.bot.hasGreeted = true
        if Float.random(in: 0..<1) < 0.25 { post(bot, greetings.randomElement()!, game: game) }
    }

    /// New target acquired: 10% targeted insult (Cannon.java:1132-1134 rate).
    static func maybeInsult(_ bot: Cannon, target: Cannon, game: GameController) {
        guard bot.bot.hasGreeted, Float.random(in: 0..<1) < 0.1 else { return }
        post(bot, "\(insults.randomElement()!) \(target.name)!", game: game)
    }

    /// The bot just died: 25% pained cry (Cannon.java:1131-1135).
    static func maybeDeathCry(_ bot: Cannon, game: GameController) {
        if Float.random(in: 0..<1) < 0.25 { post(bot, deaths.randomElement()!, game: game) }
    }

    /// The bot's shot just killed someone: 25% boast (Weapon.java:1057-1059).
    static func maybeBoast(_ bot: Cannon, game: GameController) {
        if Float.random(in: 0..<1) < 0.25 { post(bot, kills.randomElement()!, game: game) }
    }

    /// The human player killed someone: each bot 10% compliment
    /// (Network.pollBotsForCompliments, Weapon.java:1122-1124).
    static func pollCompliments(for human: Cannon, game: GameController) {
        for b in game.players where b.isBot && b.active {
            if Float.random(in: 0..<1) < 0.1 {
                post(b, "\(compliments.randomElement()!) \(human.name)!", game: game)
            }
        }
    }
}
