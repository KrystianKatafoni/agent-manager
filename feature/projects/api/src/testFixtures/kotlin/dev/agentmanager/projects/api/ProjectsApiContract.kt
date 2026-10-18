package dev.agentmanager.projects.api

/** Contract every [ProjectsApi] implementation (fake and real) must satisfy. */
public abstract class ProjectsApiContract {
    protected abstract fun create(): ProjectsApi
}
