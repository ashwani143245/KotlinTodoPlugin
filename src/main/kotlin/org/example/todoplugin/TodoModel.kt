package org.example.todoplugin

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

// Data class to represent a TODO item
data class TodoItem(
    val text: String,
    val lineNumber: Int,
    val filePath: String
)

// Service to persist TODO state between IDE restarts
@Service
@State(
    name = "TodoStateService",
    storages = [Storage("todoPlugin.xml")]
)
class TodoStateService : PersistentStateComponent<TodoStateService.State> {
    data class State(
        var todos: List<TodoItem> = emptyList(),
        var filterKeyword: String = ""
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun setTodos(todos: List<TodoItem>) {
        myState.todos = todos
    }

    fun getTodos(): List<TodoItem> = myState.todos

    fun setFilterKeyword(keyword: String) {
        myState.filterKeyword = keyword
    }

    fun getFilterKeyword(): String = myState.filterKeyword
}