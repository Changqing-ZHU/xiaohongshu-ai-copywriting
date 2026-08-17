package com.example.xhscopywriting.service;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import org.springframework.stereotype.Component;

import com.example.xhscopywriting.exception.UrlContentException;

@Component
public class PublicUrlDestinationValidator implements UrlDestinationValidator {

    @Override
    public void validate(URI uri) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
            if (addresses.length == 0) {
                throw failure(null);
            }
            for (InetAddress address : addresses) {
                byte[] bytes = address.getAddress();
                boolean uniqueLocalIpv6 = bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC;
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()
                        || uniqueLocalIpv6) {
                    throw failure(null);
                }
            }
        } catch (UnknownHostException exception) {
            throw failure(exception);
        }
    }

    private UrlContentException failure(Throwable cause) {
        String message = DefaultUrlContentService.SAFE_ACCESS_FAILURE_MESSAGE;
        return cause == null
                ? new UrlContentException(message)
                : new UrlContentException(message, cause);
    }
}
