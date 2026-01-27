## Project Overview

SuperSonic is a chat-based data analytics platform that converts natural language queries to SQL using LLM integration. The system supports multiple LLM providers and implements a hybrid parsing approach combining rule-based and LLM-based strategies for natural language to SQL conversion.

## Architecture

### Core Components

- **LLM Integration Layer**: Uses LangChain4j for orchestrating multiple LLM providers (OpenAI, Ollama, Dify) ModelProvider.java:16-44
- **NL2SQL Parser**: Multi-stage parsing system with rule-based and LLM-based strategies NL2SQLParser.java:1-50
- **SQL Generation**: Self-consistency voting mechanism for reliable SQL generation OnePassSCSqlGenStrategy.java:1-32
- **SQL Correction**: Post-generation validation and optimization LLMPhysicalSqlCorrector.java:25-50

### Key Modules

1. **Chat Module** (`chat/`): Handles user interactions, query parsing, and execution
2. **Headless Module** (`headless/`): Core semantic parsing and SQL generation logic
3. **Common Module** (`common/`): Shared utilities and configurations
4. **Web App** (`webapp/`): Frontend interface

## LLM Configuration

### Supported Providers

- **OpenAI**: GPT models with custom endpoints OpenAiModelFactory.java:15-39
- **Ollama**: Local model deployment LLMConfigUtils.java:34-62
- **Dify**: Custom API integration DifyAiChatModel.java:21-55

### Configuration Parameters

Key parameters for LLM configuration ChatModelParameters.java:14-42 :

- `provider`: LLM provider type
- `baseUrl`: API endpoint URL
- `apiKey`: Authentication key
- `modelName`: Model identifier
- `temperature`: Response randomness (0.0-1.0)
- `timeOut`: Request timeout in seconds

## Parsing Strategy

### Two-Phase Approach

1. **Rule-Based Parsing**: Progressive matching modes (STRICT → MODERATE → LOOSE)
2. **LLM-Based Parsing**: Self-consistency generation with exemplar retrieval

### Key Classes

- `NL2SQLParser`: Main orchestration class NL2SQLParser.java:77-156
- `OnePassSCSqlGenStrategy`: Self-consistency SQL generation OnePassSCSqlGenStrategy.java:78-122
- `LLMSqlCorrector`: SQL validation and correction

## Development Workflow

### Setting up LLM Configuration

1. Create `ChatModel` configuration through API or programmatically S2BaseDemo.java:142-163
2. Configure Agent with ChatApp settings
3. Test connection using provided utilities system.ts:3-8

### Adding New LLM Providers

1. Implement `ModelFactory` interface
2. Register provider in `ModelProvider.add()` ModelProvider.java:24-26
3. Add configuration parameters to `ChatModelParameters` ChatModelParameters.java:44-47

### Extending Parsing Strategies

1. Implement new strategy in `headless/chat/parser/llm/`
2. Register as ChatApp with unique APP_KEY
3. Configure in Agent's chatAppConfig

## Key Files for Development

| File                           | Purpose                                                      |
| ------------------------------ | ------------------------------------------------------------ |
| `ModelProvider.java`           | LLM provider factory and configuration ModelProvider.java:28-44 |
| `LLMConfigUtils.java`          | Pre-configured LLM settings for testing LLMConfigUtils.java:28-103 |
| `OnePassSCSqlGenStrategy.java` | Core SQL generation logic OnePassSCSqlGenStrategy.java:44-57 |
| `NL2SQLParser.java`            | Main parsing orchestration NL2SQLParser.java:93-123          |

## Dependencies

Core dependencies include pom.xml:109-149 :

- `langchain4j`: LLM integration framework
- `langchain4j-open-ai`: OpenAI provider
- `langchain4j-local-ai`: Local model support
- `langchain4j-chroma`: Vector database for embeddings

## Testing

Use `LLMConfigUtils` for testing different LLM providers LLMConfigUtils.java:6-26 :

- OpenAI GPT models
- Ollama local models (Llama3, Qwen2.5)
- Custom API endpoints