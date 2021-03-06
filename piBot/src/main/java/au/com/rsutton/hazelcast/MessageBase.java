package au.com.rsutton.hazelcast;

import java.io.Serializable;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;

public abstract class MessageBase<M> implements Serializable
{

	
	private static final long serialVersionUID = 1130437880736641457L;
	protected transient ITopic<M> topicInstance;
	final transient private HcTopic topic;

	protected MessageBase(HcTopic topic)
	{
		this.topic = topic;
	}

	@SuppressWarnings("unchecked")
	public void publish()
	{
		if (topicInstance==null)
		{
			this.topicInstance = HazelCastInstance.getInstance().getTopic(topic.toString());

		}
		topicInstance.publish((M) this);
	}

	public void addMessageListener(MessageListener<M> listener)
	{
		if (topicInstance==null)
		{
			this.topicInstance = HazelCastInstance.getInstance().getTopic(topic.toString());

		}
		topicInstance.addMessageListener(listener);
	}
}
