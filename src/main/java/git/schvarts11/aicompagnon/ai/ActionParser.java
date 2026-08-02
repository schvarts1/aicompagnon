package git.schvarts11.aicompagnon.ai;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a single-line AI response into a structured {@link Action}.
 *
 * <p>Expected format:
 * <pre>
 * ACTION: &lt;type&gt; [args...]
 * </pre>
 */
public final class ActionParser {

    private static final Pattern ACTION_PATTERN = Pattern.compile(
            "^ACTION:\\s*(\\w+)(?:\\s+(.*))?$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private ActionParser() {
    }

    /**
     * Parses the AI response text into an Action.
     *
     * @param aiResponseText raw text from the AI
     * @return parsed Action, or {@link Action.Wait} with reason "parse_failed" on failure
     */
    public static Action parse(String aiResponseText) {
        if (aiResponseText == null || aiResponseText.isBlank()) {
            return Action.waitAction("empty_response");
        }

        String firstLine = aiResponseText.lines().findFirst().orElse(aiResponseText).trim();
        Matcher matcher = ACTION_PATTERN.matcher(firstLine);
        if (!matcher.find()) {
            return Action.waitAction("no_action_prefix");
        }

        String type = matcher.group(1).toLowerCase();
        String args = matcher.group(2) != null ? matcher.group(2).trim() : "";

        return switch (type) {
            case "move_to" -> parseMoveTo(args);
            case "follow" -> parseFollow(args);
            case "look_at" -> parseLookAt(args);
            case "say" -> parseSay(args, aiResponseText);
            case "attack" -> parseAttack(args);
            case "wander" -> Action.Wander.INSTANCE;
            case "wait" -> Action.waitAction(args.isEmpty() ? "explicit_wait" : args);
            default -> Action.waitAction("unknown_action:" + type);
        };
    }

    private static Action parseMoveTo(String args) {
        String[] parts = args.split("\\s+");
        if (parts.length < 3) {
            return Action.waitAction("move_to_insufficient_args");
        }
        try {
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            return new Action.MoveTo(x, y, z);
        } catch (NumberFormatException e) {
            return Action.waitAction("move_to_invalid_numbers");
        }
    }

    private static Action parseFollow(String args) {
        String name = args.isEmpty() ? "" : args.split("\\s+")[0];
        if (name.isBlank()) {
            return Action.waitAction("follow_missing_name");
        }
        return new Action.Follow(name);
    }

    private static Action parseLookAt(String args) {
        String[] parts = args.split("\\s+");
        if (parts.length < 3) {
            return Action.waitAction("look_at_insufficient_args");
        }
        try {
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            return new Action.LookAt(x, y, z);
        } catch (NumberFormatException e) {
            return Action.waitAction("look_at_invalid_numbers");
        }
    }

    private static Action parseSay(String firstLineArgs, String fullText) {
        // Say can have spaces; prefer everything after the type keyword on the same line,
        // or fall back to the rest of the text after the first line.
        String message = firstLineArgs;
        if (message.isBlank() && fullText.contains("\n")) {
            message = fullText.substring(fullText.indexOf('\n') + 1).trim();
        }
        if (message.isBlank()) {
            return Action.waitAction("say_empty");
        }
        return new Action.Say(message);
    }

    private static Action parseAttack(String args) {
        String name = args.isEmpty() ? "" : args.split("\\s+")[0];
        if (name.isBlank()) {
            return Action.waitAction("attack_missing_name");
        }
        return new Action.Attack(name);
    }
}
