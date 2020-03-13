/*
 * Based on [https://github.com/daemontus/kotlin-ace-wrapper]
 */
package ace

@JsModule("kotlin-ace-loader!?id=ace/editor&name=Editor")
@JsNonModule
external class Editor(
        renderer: VirtualRenderer,
        session: EditSession,
        options:Any?
)  {
    fun getValue(): String
    fun setValue(value: String, cursorPos: Int)
    fun getSession(): EditSession
    fun setOption(option: String, module: dynamic)

}