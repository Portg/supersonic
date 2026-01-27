import { PlusCircleOutlined } from '@ant-design/icons';
import { AgentType } from '../type';
import styles from './style.module.less';
import classNames from 'classnames';
import IconFont from '../../components/IconFont';
import { AGENT_ICONS } from '../constants';

type Props = {
  agentList: AgentType[];
  currentAgent?: AgentType;
  onSelectAgent: (agent: AgentType) => void;
  onAddAgent?: () => void;
};

const AgentList: React.FC<Props> = ({ agentList, currentAgent, onSelectAgent, onAddAgent }) => {
  const handleAddAgent = () => {
    if (onAddAgent) {
      onAddAgent();
    } else {
      // 默认行为：在当前窗口打开助手管理页面
      window.location.href = '/agent';
    }
  };

  return (
    <div className={styles.agentList}>
      <div className={styles.header}>
        <div className={styles.headerTitle}>智能助理</div>
        <PlusCircleOutlined className={styles.plusIcon} onClick={handleAddAgent} />
      </div>
      <div className={styles.agentListContent}>
        {agentList.map((agent, index) => {
          const agentItemClass = classNames(styles.agentItem, {
            [styles.active]: currentAgent?.id === agent.id,
          });
          return (
            <div
              key={agent.id}
              className={agentItemClass}
              onClick={() => {
                onSelectAgent(agent);
              }}
            >
              <IconFont type={AGENT_ICONS[index % AGENT_ICONS.length]} className={styles.avatar} />
              <div className={styles.agentInfo}>
                <div className={styles.agentName}>{agent.name}</div>
                <div className={styles.agentDesc}>{agent.description}</div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default AgentList;
