# Implementation Plan: Pure Stateless Security & 10s Timeout

This plan details the updated implementation of a highly secure stateless protocol for the Tic-Tac-Toe application. Based on our architectural analysis, we are introducing a Game ID and a strict 10-second Time-To-Live (TTL) limit per turn, while explicitly omitting the redundant `TurnNumber`.

## Goal Description
We will modify the Client-Server communication protocol to incorporate advanced cryptographic stateless verification using only mathematically necessary fields. By embedding `GameID` and `TurnStartTime` into the payload and HMAC signature, we will:
1. Prevent Cross-game Replay (Dictionary Attacks) using `GameID`.
2. Enforce a strict 10-second server-side limit for the human player to make a decision using `TurnStartTime`.
3. Eliminate redundant data (`TurnNumber`) to keep the payload minimal and conceptually pure.

## Proposed Changes

### `Server.java`
- Modify the `START` command to generate a unique `GameID` (UUID) and a `TurnStartTime` (timestamp in milliseconds).
- Modify the `generateHMAC` method indirectly by feeding it a new, robust payload: `HMAC(GameID + BoardLine + TurnStartTime)`.
- Modify the `MOVE` command handler to expect the new payload format: `MOVE <gameId> <boardLine> <turnStartTime> <humanMove> <signature>`.
- Add validation logic:
  - Verify the signature matches the expanded payload.
  - Check if `System.currentTimeMillis() - turnStartTime > 10000`. If so, return a `timeout` response.
- Update response formatting to return all state variables for the next turn.

#### [MODIFY] [Server.java](file:///c:/Users/phucn/OneDrive/Documents/tictactoe/tttbasic/src/main/java/vgu/pe2026/ttt/basis/Server.java)
- Update `handleMove` parsing (expect 6 parts instead of 4).
- Add timeout verification.
- Extract response generation into a helper method `generateResponse` to ensure consistent state packing without `TurnNumber`.

### `Client.java`
- Update the main game loop to parse and locally store the new state variables (`GameID`, `TurnStartTime`) returned from the `START` command.
- Update the `send` string for the `MOVE` request to include the new variables.
- Add response handling for the new `timeout` status, informing the user they took too long and ending the game.

#### [MODIFY] [Client.java](file:///c:/Users/phucn/OneDrive/Documents/tictactoe/tttbasic/src/main/java/vgu/pe2026/ttt/basis/Client.java)
- Update `main` method parsing arrays (expecting length 6).
- Inject `GameID` and `TurnStartTime` into the `send()` method call.
- Handle "timeout" status cleanly on the console.

## User Review Required
> [!IMPORTANT]
> The timeout logic is enforced entirely on the **Server**. The Client will still wait for you to type a move in the console. If you take 15 seconds to type and hit Enter, the Client will send it, and the Server will instantly reject it with a Timeout message. This is standard for CLI stateless apps.

## Verification Plan
1. **Automated Check:** Compile and run `Server.java` and `Client.java`.
2. **Normal Gameplay:** Verify playing a normal game within 10s works as expected.
3. **Timeout Test:** Wait 12 seconds before entering a move. Verify the Server rejects the move and the Client prints a timeout message.
4. **Invalid Move Test:** Try placing a move on an occupied cell. Verify the server rejects it and the time limit is NOT reset (you still have the remaining original 10s to make a correct move).
