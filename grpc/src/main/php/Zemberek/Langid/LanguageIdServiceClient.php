<?php
// GENERATED CODE -- DO NOT EDIT!

namespace Zemberek\Langid;

/**
 */
class LanguageIdServiceClient extends \Grpc\BaseStub {

    /**
     * @param string $hostname hostname
     * @param array $opts channel options
     * @param \Grpc\Channel $channel (optional) re-use channel object
     */
    public function __construct($hostname, $opts, $channel = null) {
        parent::__construct($hostname, $opts, $channel);
    }

    /**
     * @param \Zemberek\Langid\DetectRequest $argument input argument
     * @param array $metadata metadata
     * @param array $options call options
     */
    public function Detect(\Zemberek\Langid\DetectRequest $argument,
      $metadata = [], $options = []) {
        return $this->_simpleRequest('/zemberek.langid.LanguageIdService/Detect',
        $argument,
        ['\Zemberek\Langid\DetectResponse', 'decode'],
        $metadata, $options);
    }

    /**
     * @param \Zemberek\Langid\DetectRequest $argument input argument
     * @param array $metadata metadata
     * @param array $options call options
     */
    public function DetectFast(\Zemberek\Langid\DetectRequest $argument,
      $metadata = [], $options = []) {
        return $this->_simpleRequest('/zemberek.langid.LanguageIdService/DetectFast',
        $argument,
        ['\Zemberek\Langid\DetectResponse', 'decode'],
        $metadata, $options);
    }

}
