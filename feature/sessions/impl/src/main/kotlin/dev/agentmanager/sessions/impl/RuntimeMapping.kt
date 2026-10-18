package dev.agentmanager.sessions.impl

import dev.agentmanager.agentruntime.api.RuntimeError
import dev.agentmanager.agentruntime.api.RuntimeSession
import dev.agentmanager.sessions.api.RuntimeProblem
import dev.agentmanager.sessions.api.SessionState

/** FR-S2, docs/05: supervisor phase and waiting reason → state. Details (question, tool) come with hooks (AM-603). */
internal fun RuntimeSession.toSessionState(): SessionState = TODO("AM-402")

/** Fixed category of a listing failure (ENG-C8: drops exit codes, tails and details). */
internal fun RuntimeError.toProblem(): RuntimeProblem = TODO("AM-402")
