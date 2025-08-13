/**
 * AI对话页面核心JavaScript文件
 * 实现流式对话、消息渲染、提示组件等功能
 */

// 全局变量
let isStreaming = false;
let currentEventSource = null;
let messageCounter = 0;

/**
 * 页面加载完成后初始化
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('AI对话助手已加载');
    showTip('欢迎使用AI对话助手！点击右下角的小兔子获取使用提示 🐰', 'info');
});

/**
 * 处理键盘按键事件
 * @param {Event} event - 键盘事件
 */
function handleKeyPress(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}

/**
 * 发送消息主函数
 */
function sendMessage() {
    const messageInput = document.getElementById('messageInput');
    const message = messageInput.value.trim();
    
    if (!message) {
        showTip('请输入消息内容', 'warning');
        return;
    }
    
    if (isStreaming) {
        showTip('正在处理中，请稍候...', 'warning');
        return;
    }
    
    // 添加用户消息到聊天容器
    addUserMessage(message);
    
    // 清空输入框
    messageInput.value = '';
    
    // 开始流式请求
    startStreamingRequest(message);
}

/**
 * 添加用户消息到聊天界面
 * @param {string} message - 用户消息内容
 */
function addUserMessage(message) {
    const chatContainer = document.getElementById('chatContainer');
    const messageDiv = document.createElement('div');
    messageDiv.className = 'flex items-start space-x-3 mb-4 justify-end';
    
    messageDiv.innerHTML = `
        <div class="bg-gradient-to-r from-blue-500 to-purple-600 text-white rounded-lg px-4 py-2 max-w-2xl">
            <p class="text-sm">${escapeHtml(message)}</p>
        </div>
        <div class="w-8 h-8 bg-gradient-to-r from-blue-400 to-purple-500 rounded-full flex items-center justify-center flex-shrink-0">
            <span class="text-white text-sm">👤</span>
        </div>
    `;
    
    chatContainer.appendChild(messageDiv);
    scrollToBottom();
}

/**
 * 添加AI消息到聊天界面
 * @param {string} messageId - 消息ID
 * @returns {HTMLElement} 消息元素
 */
function addAIMessage(messageId) {
    const chatContainer = document.getElementById('chatContainer');
    const messageDiv = document.createElement('div');
    messageDiv.className = 'flex items-start space-x-3 mb-4';
    messageDiv.id = `ai-message-${messageId}`;
    
    messageDiv.innerHTML = `
        <div class="w-8 h-8 bg-gradient-to-r from-green-400 to-blue-500 rounded-full flex items-center justify-center flex-shrink-0">
            <span class="text-white text-sm">🤖</span>
        </div>
        <div class="bg-gray-100 rounded-lg px-4 py-2 max-w-4xl">
            <div id="ai-content-${messageId}" class="text-gray-800 text-sm">
                <span class="typing-indicator">正在思考中...</span>
            </div>
        </div>
    `;
    
    chatContainer.appendChild(messageDiv);
    scrollToBottom();
    return messageDiv;
}

/**
 * 更新AI消息内容
 * @param {string} messageId - 消息ID
 * @param {string} content - 消息内容
 * @param {boolean} isComplete - 是否完成
 */
function updateAIMessage(messageId, content, isComplete = false) {
    const contentElement = document.getElementById(`ai-content-${messageId}`);
    if (contentElement) {
        const { thinkContent, actualContent } = parseThinkAndContent(content);
        
        let html = '';
        
        // 如果有思考内容
        if (thinkContent) {
            if (isComplete) {
                // 完成时显示可折叠的思考过程
                html += `
                    <div class="mb-2">
                        <button onclick="toggleThinking('${messageId}')" class="text-xs text-gray-500 hover:text-gray-700 flex items-center">
                            <span id="think-toggle-${messageId}">▶</span>
                            <span class="ml-1">思考过程</span>
                        </button>
                        <div id="think-content-${messageId}" class="hidden mt-1 p-2 bg-gray-50 rounded text-xs text-gray-600 italic border-l-2 border-gray-300">
                            ${escapeHtml(thinkContent)}
                        </div>
                    </div>
                `;
            } else {
                // 进行中时显示思考状态
                html += `
                    <div class="mb-2 p-2 bg-yellow-50 rounded text-xs text-gray-500 italic border-l-2 border-yellow-300">
                        <span class="text-yellow-600">🤔 思考中...</span>
                        <div class="mt-1 text-gray-400">${escapeHtml(thinkContent)}</div>
                    </div>
                `;
            }
        }
        
        // 显示实际回答内容
        if (actualContent || isComplete) {
            const renderedContent = actualContent ? renderMarkdown(actualContent) : '<span class="text-gray-500 italic">AI暂时没有回复</span>';
            html += `<div class="text-gray-800 markdown-content">${renderedContent}</div>`;
        }
        
        // 添加输入指示器
        if (!isComplete) {
            html += '<span class="typing-indicator ml-1">▋</span>';
        }
        
        contentElement.innerHTML = html;
        scrollToBottom();
    }
}

/**
 * 开始流式请求
 * @param {string} message - 用户消息
 */
function startStreamingRequest(message) {
    const model = document.getElementById('modelSelect').value;
    const apiUrl = `https://ai.jasonlat.com/api/v1/ollama/generate_stream?model=${encodeURIComponent(model)}&message=${encodeURIComponent(message)}`;
    
    isStreaming = true;
    updateSendButton(true);
    
    const messageId = ++messageCounter;
    const aiMessageElement = addAIMessage(messageId);
    let accumulatedContent = '';
    
    try {
        // 创建EventSource连接
        currentEventSource = new EventSource(apiUrl);
        
        // 处理消息接收
        currentEventSource.onmessage = function(event) {
            try {
                const data = JSON.parse(event.data);
                
                // 从响应中提取内容
                if (data.result && data.result.output && data.result.output.content) {
                    accumulatedContent += data.result.output.content;
                    updateAIMessage(messageId, accumulatedContent, false);
                }
                
                // 检查是否完成
                if (data.result && data.result.metadata && data.result.metadata.finishReason === 'STOP') {
                    updateAIMessage(messageId, accumulatedContent, true);
                    closeEventSource();
                    showTip('消息接收完成！', 'success');
                }
            } catch (error) {
                console.error('解析响应数据错误:', error);
                showTip('响应数据解析失败', 'error');
            }
        };
        
        // 处理连接打开
        currentEventSource.onopen = function(event) {
            console.log('EventSource连接已建立');
        };
        
        // 处理错误
        currentEventSource.onerror = function(event) {
            console.error('EventSource错误:', event);
            updateAIMessage(messageId, accumulatedContent || '抱歉，服务暂时不可用，请稍后重试。', true);
            closeEventSource();
            showTip('回答完毕', 'info');
        };
        
    } catch (error) {
        console.error('创建EventSource失败:', error);
        updateAIMessage(messageId, '连接失败，请稍后重试。', true);
        closeEventSource();
        showTip('无法建立连接', 'error');
    }
}

/**
 * 关闭EventSource连接
 */
function closeEventSource() {
    if (currentEventSource) {
        currentEventSource.close();
        currentEventSource = null;
    }
    isStreaming = false;
    updateSendButton(false);
}

/**
 * 更新发送按钮状态
 * @param {boolean} streaming - 是否正在流式传输
 */
function updateSendButton(streaming) {
    const sendButton = document.getElementById('sendButton');
    const sendButtonText = document.getElementById('sendButtonText');
    const sendButtonIcon = document.getElementById('sendButtonIcon');
    
    if (streaming) {
        sendButton.disabled = true;
        sendButton.className = sendButton.className.replace('hover:from-blue-600 hover:to-purple-700', '');
        sendButtonText.textContent = '发送中';
        sendButtonIcon.textContent = '⏳';
    } else {
        sendButton.disabled = false;
        sendButton.className += ' hover:from-blue-600 hover:to-purple-700';
        sendButtonText.textContent = '发送';
        sendButtonIcon.textContent = '🚀';
    }
}

/**
 * 滚动到聊天容器底部
 */
function scrollToBottom() {
    const chatContainer = document.getElementById('chatContainer');
    chatContainer.scrollTop = chatContainer.scrollHeight;
}

/**
 * HTML转义函数
 * @param {string} text - 需要转义的文本
 * @returns {string} 转义后的文本
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * 简单的Markdown渲染函数
 * @param {string} text - Markdown文本
 * @returns {string} 渲染后的HTML
 */
function renderMarkdown(text) {
    if (!text) return '';
    
    let html = text;
    
    // 处理代码块 (```)
    html = html.replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>');
    
    // 处理行内代码 (`)
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
    
    // 处理标题
    html = html.replace(/^### (.*$)/gm, '<h3>$1</h3>');
    html = html.replace(/^## (.*$)/gm, '<h2>$1</h2>');
    html = html.replace(/^# (.*$)/gm, '<h1>$1</h1>');
    
    // 处理粗体
    html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    
    // 处理斜体
    html = html.replace(/\*(.*?)\*/g, '<em>$1</em>');
    
    // 处理换行
    html = html.replace(/\n/g, '<br>');
    
    // 处理列表
    html = html.replace(/^- (.*$)/gm, '<li>$1</li>');
    html = html.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>');
    
    return html;
}

/**
 * 解析think标签和实际内容
 * @param {string} content - 原始内容
 * @returns {Object} 包含thinkContent和actualContent的对象
 */
function parseThinkAndContent(content) {
    if (!content) {
        return { thinkContent: '', actualContent: '' };
    }
    
    // 提取think标签内容
    const thinkMatch = content.match(/<think>(.*?)<\/think>/s);
    const thinkContent = thinkMatch ? thinkMatch[1].trim() : '';
    
    // 移除think标签，获取实际内容
    const actualContent = content.replace(/<think>.*?<\/think>/gs, '').trim();
    
    return { thinkContent, actualContent };
}

/**
 * 切换思考过程的显示/隐藏
 * @param {string} messageId - 消息ID
 */
function toggleThinking(messageId) {
    const thinkContent = document.getElementById(`think-content-${messageId}`);
    const toggleIcon = document.getElementById(`think-toggle-${messageId}`);
    
    if (thinkContent && toggleIcon) {
        if (thinkContent.classList.contains('hidden')) {
            thinkContent.classList.remove('hidden');
            toggleIcon.textContent = '▼';
        } else {
            thinkContent.classList.add('hidden');
            toggleIcon.textContent = '▶';
        }
    }
}

/**
 * 显示提示消息
 * @param {string} message - 提示消息
 * @param {string} type - 提示类型 (success, error, warning, info)
 */
function showTip(message, type = 'info') {
    const tipContainer = document.getElementById('tipContainer');
    const tipId = 'tip-' + Date.now();
    
    // 定义不同类型的样式
    const typeStyles = {
        success: 'bg-green-500 border-green-600',
        error: 'bg-red-500 border-red-600',
        warning: 'bg-yellow-500 border-yellow-600',
        info: 'bg-blue-500 border-blue-600'
    };
    
    const tipElement = document.createElement('div');
    tipElement.id = tipId;
    tipElement.className = `${typeStyles[type]} text-white px-4 py-2 rounded-lg shadow-lg mb-2 transform transition-all duration-300 translate-x-full opacity-0`;
    tipElement.innerHTML = `
        <div class="flex items-center space-x-2">
            <span class="text-sm">${escapeHtml(message)}</span>
            <button onclick="removeTip('${tipId}')" class="text-white hover:text-gray-200 ml-2">
                <span class="text-xs">✕</span>
            </button>
        </div>
    `;
    
    tipContainer.appendChild(tipElement);
    
    // 动画显示
    setTimeout(() => {
        tipElement.className = tipElement.className.replace('translate-x-full opacity-0', 'translate-x-0 opacity-100');
    }, 10);
    
    // 自动移除
    setTimeout(() => {
        removeTip(tipId);
    }, 5000);
}

/**
 * 移除提示消息
 * @param {string} tipId - 提示消息ID
 */
function removeTip(tipId) {
    const tipElement = document.getElementById(tipId);
    if (tipElement) {
        tipElement.className = tipElement.className.replace('translate-x-0 opacity-100', 'translate-x-full opacity-0');
        setTimeout(() => {
            if (tipElement.parentNode) {
                tipElement.parentNode.removeChild(tipElement);
            }
        }, 300);
    }
}

/**
 * 显示随机使用提示
 */
function showRandomTip() {
    const tips = [
        '💡 提示：你可以按Enter键快速发送消息',
        '🎯 提示：尝试切换不同的AI模型体验不同的对话风格',
        '⚡ 提示：AI正在实时生成回复，请耐心等待',
        '🌟 提示：可以问我任何问题，我会尽力帮助你',
        '🔄 提示：如果遇到问题，可以刷新页面重新开始',
        '📝 提示：支持多行输入，按Shift+Enter换行'
    ];
    
    const randomTip = tips[Math.floor(Math.random() * tips.length)];
    showTip(randomTip, 'info');
}

/**
 * 页面卸载时清理资源
 */
window.addEventListener('beforeunload', function() {
    closeEventSource();
});