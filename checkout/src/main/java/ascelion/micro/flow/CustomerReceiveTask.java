package ascelion.micro.flow;

import java.util.UUID;

import ascelion.micro.checkout.api.CheckoutChannel;
import ascelion.micro.customer.api.Customer;
import ascelion.micro.shared.message.MessagePayload;

import static ascelion.micro.checkout.api.CheckoutChannel.CUSTOMER_MESSAGE;
import static ascelion.micro.flow.CheckoutConstants.CUSTOMER_RECEIVE_TASK;
import static ascelion.micro.shared.message.MessageSenderAdapter.HEADER_CORRELATION;
import static ascelion.micro.shared.message.MessageSenderAdapter.HEADER_KIND;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service(CUSTOMER_RECEIVE_TASK)
@EnableBinding(CheckoutChannel.class)
public class CustomerReceiveTask extends AbstractReceiveTask<Customer> {

	@Override
	@StreamListener(target = CheckoutChannel.INPUT, condition = "headers." + HEADER_KIND + " == '" + CUSTOMER_MESSAGE + "_RESPONSE'")
	public void messageReceived(@Payload MessagePayload<Customer> payload, @Header(HEADER_CORRELATION) UUID pid, @Header(HEADER_KIND) String kind) {
		received(payload, pid, kind);
	}
}
