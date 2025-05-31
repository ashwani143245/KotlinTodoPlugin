package org.example.todoplugin

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.event.ListSelectionEvent

class TodoToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val todoPanel = TodoPanel(project)
        val content = ContentFactory.getInstance().createContent(todoPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class TodoPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val todoStateService = service<TodoStateService>()
    private val listModel = DefaultListModel<TodoItem>()
    private val todoList = JBList(listModel)
    private val filterField = JBTextField()

    init {
        // Set up the UI
        setupUI()
        
        // Set up listeners
        setupListeners()
        
        // Initialize with the current file
        val currentFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        if (currentFile != null) {
            updateTodoList(currentFile.path)
        }
        
        // Restore filter from persistent state
        filterField.text = todoStateService.getFilterKeyword()
    }

    private fun setupUI() {
        // Set up the filter field
        val filterPanel = JPanel(BorderLayout())
        filterPanel.add(JLabel("Filter: "), BorderLayout.WEST)
        filterPanel.add(filterField, BorderLayout.CENTER)
        filterPanel.border = JBUI.Borders.empty(5)
        
        // Set up the TODO list
        todoList.cellRenderer = TodoListCellRenderer()
        todoList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        
        // Add components to the panel
        add(filterPanel, BorderLayout.NORTH)
        add(JBScrollPane(todoList), BorderLayout.CENTER)
    }

    private fun setupListeners() {
        // Listen for filter changes
        filterField.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                updateFilter()
            }
        })
        
        // Listen for list selection
        todoList.addListSelectionListener { e: ListSelectionEvent ->
            if (!e.valueIsAdjusting && todoList.selectedIndex >= 0) {
                navigateToTodo(todoList.selectedValue)
            }
        }
        
        // Listen for file changes
        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: com.intellij.openapi.vfs.VirtualFile) {
                    updateTodoList(file.path)
                }
                
                override fun fileClosed(source: FileEditorManager, file: com.intellij.openapi.vfs.VirtualFile) {
                    // Clear the list if no files are open
                    if (source.selectedFiles.isEmpty()) {
                        listModel.clear()
                    }
                }
                
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    event.newFile?.let { updateTodoList(it.path) }
                }
            }
        )
    }


    private fun updateFilter() {
        val filterKeyword = filterField.text
        todoStateService.setFilterKeyword(filterKeyword)
        
        // Re-apply filter to current TODOs
        val currentFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        if (currentFile != null) {
            updateTodoList(currentFile.path)
        }
    }

    private fun updateTodoList(filePath: String) {
        val file = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(filePath)
        if (file != null && file.extension == "kt") {
            // Scan the file for TODOs
            val todos = TodoScanner.scanFile(file)
            
            // Filter TODOs based on the filter keyword
            val filterKeyword = todoStateService.getFilterKeyword()
            val filteredTodos = if (filterKeyword.isNotEmpty()) {
                todos.filter { it.text.contains(filterKeyword, ignoreCase = true) }
            } else {
                todos
            }
            
            // Update the list model
            listModel.clear()
            filteredTodos.forEach { listModel.addElement(it) }
            
            // Update the persistent state
            todoStateService.setTodos(filteredTodos)
        } else {
            listModel.clear()
            todoStateService.setTodos(emptyList())
        }
    }

    private fun navigateToTodo(todoItem: TodoItem) {
        val file = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(todoItem.filePath)
        if (file != null) {
            // Open the file in the editor
            val fileEditorManager = FileEditorManager.getInstance(project)
            val textEditor = fileEditorManager.openFile(file, true)
                .filterIsInstance<com.intellij.openapi.fileEditor.TextEditor>()
                .firstOrNull()
                
            if (textEditor != null) {
                // Navigate to the line
                val editor = textEditor.editor
                val document = editor.document
                val lineStartOffset = document.getLineStartOffset(todoItem.lineNumber - 1)
                
                // Move caret to the line and scroll to it
                editor.caretModel.moveToOffset(lineStartOffset)
                editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER)
            }
        }
    }
}

class TodoListCellRenderer : ListCellRenderer<TodoItem> {
    private val panel = JPanel(BorderLayout())
    private val lineLabel = JLabel()
    private val todoLabel = JLabel()
    
    init {
        panel.add(lineLabel, BorderLayout.WEST)
        panel.add(todoLabel, BorderLayout.CENTER)
        panel.border = JBUI.Borders.empty(2)
        lineLabel.border = JBUI.Borders.emptyRight(10)
    }
    
    override fun getListCellRendererComponent(
        list: JList<out TodoItem>,
        value: TodoItem,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        // Set up the line number label
        lineLabel.text = "Line ${value.lineNumber}:"
        
        // Set up the TODO text label
        todoLabel.text = value.text
        todoLabel.icon = com.intellij.icons.AllIcons.General.TodoDefault
        
        // Set up the tooltip
        panel.toolTipText = "${value.filePath}:${value.lineNumber}"
        
        // Handle selection state
        if (isSelected) {
            panel.background = list.selectionBackground
            panel.foreground = list.selectionForeground
            lineLabel.foreground = list.selectionForeground
            todoLabel.foreground = list.selectionForeground
        } else {
            panel.background = list.background
            panel.foreground = list.foreground
            lineLabel.foreground = list.foreground
            todoLabel.foreground = list.foreground
        }
        
        return panel
    }
}