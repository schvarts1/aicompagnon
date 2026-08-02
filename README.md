# AI Compagnon

A Paper 26.1 plugin that lets players spawn AI-driven companions into the Minecraft world. Each companion periodically observes its surroundings, sends context to an OpenAI-compatible API, and executes the returned action using Paper's entity API.

## Requirements

- Paper `api-version: 26.1`+
- Java `25`
- An OpenAI-compatible chat completions endpoint (Ollama, LM Studio, OpenAI, etc.)

## Installation

1. Build with `.\gradlew.bat build`
2. Copy `build/libs/plugin.jar` into your Paper server's `plugins/` directory
3. Start the server once to generate `plugins/ai-compagnon/config.yml`
4. Edit `config.yml` with your API endpoint, model, and preferences
5. Restart or run `/compagnon reload`

## Commands

| Command | Permission | Description |
|---|---|---|
| `/compagnon spawn [name]` | `aicompagnon.spawn` | Spawn an AI companion at your location |
| `/compagnon remove <id\|all>` | `aicompagnon.admin` | Remove a companion by ID or all companions |
| `/compagnon list [player]` | `aicompagnon.spawn` | List active companions |
| `/compagnon info <id>` | `aicompagnon.spawn` | Show companion details |
| `/compagnon reload` | `aicompagnon.admin` | Reload config without restart |

`op` grants all permissions by default.

## Configuration

| Field | Default | Description |
|---|---|---|
| `api_endpoint` | `http://localhost:11434` | Base URL of the AI API |
| `api_key` | `""` | Bearer token; leave empty for local models |
| `model` | `llama3` | Model identifier |
| `tick_interval_ticks` | `60` | AI decision interval in server ticks |
| `max_companions_per_player` | `5` | Max companions a player can spawn |
| `companion_prefix` | `<[AI]> ` | Prefix prepended to companion chat |
| `remove_on_owner_quit` | `false` | Remove companion when owner quits |
| `allowed_actions` | `follow,move_to,look_at,say,wait,wander` | Enabled action whitelist |
| `api_timeout_seconds` | `15` | HTTP timeout for AI requests |
| `context_radius_blocks` | `16` | Radius for nearby entity scanning |
| `max_history_entries` | `20` | Max action history entries kept |

## How It Works

1. Player runs `/compagnon spawn`
2. Plugin spawns a persistent zombie entity with a custom name and metadata tag
3. On a configurable interval, the plugin:
   - Gathers context: position, time, weather, nearby players/entities, recent actions
   - Sends context to the AI API asynchronously
   - Parses the response into an action: `move_to`, `follow`, `look_at`, `say`, `attack`, `wander`, or `wait`
   - Executes the action on the main server thread using Paper's `Mob.getPathfinder()` API
4. On plugin disable or `/reload`, all companions are cleaned up

## Troubleshooting

**Companions don't move**
- Ensure your API is running and reachable at `api_endpoint`
- Check console for AI timeout or parse warnings
- Verify `tick_interval_ticks` is not set too high

**Permission errors**
- Grant `op` or the specific `aicompagnon.*` permission
- Permissions are declared in `paper-plugin.yml`

**API errors**
- Confirm the model name matches your API
- If using Ollama, ensure the model is pulled: `ollama pull llama3`
- Set `api_key` only if your API requires auth

## Known Limitations

- Companions are zombies with custom names; no custom skins or player models
- No persistence across server restarts yet
- Action set is limited to movement, looking, chat, and basic wandering
